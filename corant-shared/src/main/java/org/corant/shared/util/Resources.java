/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.shared.util;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ClassUtils.defaultClassLoader;
import static org.corant.shared.util.MapUtils.immutableMapOf;
import static org.corant.shared.util.ObjectUtils.forceCast;
import static org.corant.shared.util.StreamUtils.streamOf;
import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.PathUtils.GlobMatcher;

/**
 * corant-shared
 *
 * @author bingo 下午1:16:04
 *
 */
public class Resources {

  public static final Logger logger = Logger.getLogger(Resources.class.getName());

  public static <T extends URLResource> Stream<T> from(String path) throws IOException {
    if (isNotBlank(path)) {
      SourceType st = SourceType.decide(path).orElse(SourceType.CLASS_PATH);
      if (st == SourceType.FILE_SYSTEM) {
        return forceCast(fromFileSystem(path));
      } else if (st == SourceType.URL) {
        return forceCast(streamOf(fromUrl(path)));
      } else {
        return forceCast(fromClassPath(path));// default from class path;
      }
    }
    return Stream.empty();
  }

  /**
   * Use specified class loader to scan all class path resources.
   *
   * @param classLoader
   * @return
   * @throws IOException fromClassPath
   */
  public static Stream<ClassPathResource> fromClassPath(ClassLoader classLoader)
      throws IOException {
    return fromClassPath(classLoader, null);
  }

  /**
   * Scan class path resource with path, path separator is '/', allowed for use glob-pattern.
   *
   * <pre>
   * for example:
   * 1.if path is "javax/sql" then will scan all resources that under the javax.sql class path.
   * 2.if path is "java/sql/Driver.class" then will scan single resource javax.sql.Driver.
   * 3.if path is "META-INF/maven" then will scan all resources under the META-INF/maven.
   * 4.if path is blank ({@code StringUtils.isBlank}) then will scan all class path in the system.
   * 5.if path is "javax/sql/*Driver.class" then will scan javax.sql class path and filter class name
   * end with Driver.class.
   * </pre>
   *
   * @param classLoader
   * @param classPath
   * @return
   * @throws IOException fromClassPath
   */
  public static Stream<ClassPathResource> fromClassPath(ClassLoader classLoader, String classPath)
      throws IOException {
    return ClassPaths.from(classLoader, SourceType.CLASS_PATH.resolve(classPath)).stream();
  }

  /**
   * Use default class loader to scan the specified path resources.
   *
   * @param classPath
   * @return
   * @throws IOException fromClassPath
   */
  public static Stream<ClassPathResource> fromClassPath(String classPath) throws IOException {
    return fromClassPath(defaultClassLoader(), SourceType.CLASS_PATH.resolve(classPath));
  }

  /**
   * Use file create file system resource.
   *
   * @param file
   * @return
   * @throws IOException fromFileSystem
   */
  public static FileSystemResource fromFileSystem(File file) throws IOException {
    return new FileSystemResource(shouldNotNull(file));
  }

  /**
   * Use Path to find file system resource.
   *
   * @param path
   * @return
   * @throws IOException fromFileSystem
   */
  public static FileSystemResource fromFileSystem(Path path) throws IOException {
    return new FileSystemResource(shouldNotNull(path).toFile());
  }

  /**
   * Use path string to find file system resource.
   *
   * @param path
   * @return
   * @throws IOException fromFileSystem
   */
  public static Stream<FileSystemResource> fromFileSystem(String path) throws IOException {
    String usePath = SourceType.FILE_SYSTEM.resolve(path);
    if (GlobMatcher.hasGlobChar(usePath)) {
      String resolvedPath = FileUtils.resolveGlobPathPrefix(usePath);
      final GlobMatcher m = new GlobMatcher(false, true, usePath.replace('\\', '/'));
      return FileUtils.selectFiles(resolvedPath, (f) -> {
        try {
          return m.test(f.getCanonicalPath().replace('\\', '/'));
        } catch (IOException e) {
          throw new CorantRuntimeException(e);
        }
      }).stream().map(FileSystemResource::new);
    } else {
      return FileUtils.selectFiles(usePath, null).stream().map(FileSystemResource::new);
    }

  }

  /**
   * Use input stream to build input stream resource.
   *
   * @param inputStream
   * @param location
   * @return
   * @throws IOException fromInputStream
   */
  public static InputStreamResource fromInputStream(InputStream inputStream, String location)
      throws IOException {
    return new InputStreamResource(location, inputStream);
  }

  /**
   * Use specified URL string to find resource.
   *
   * @param url
   * @return
   * @throws IOException fromUrl
   */
  public static URLResource fromUrl(String url) throws IOException {
    return new URLResource(SourceType.URL.resolve(url));
  }

  /**
   * Use specified URL to find resource.
   *
   * @param url
   * @return
   * @throws IOException fromUrl
   */
  public static URLResource fromUrl(URL url) throws IOException {
    return new URLResource(url);
  }

  /**
   * Use specified http URL and proxy to find resource.
   *
   * @param url
   * @param proxy
   * @return
   * @throws IOException fromUrl
   */
  public static InputStreamResource fromUrl(URL url, Proxy proxy) throws IOException {
    return fromInputStream(url.openConnection(proxy).getInputStream(), url.toExternalForm());
  }

  /**
   * Not throw IO exception, just warning
   *
   * @see #from
   *
   * @param path
   * @return tryFrom
   */
  public static <T extends URLResource> Stream<T> tryFrom(final String path) {
    try {
      return from(path);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> String.format("Can not find resource from path %s", path));
    }
    return Stream.empty();
  }

  /**
   * Not throw IO exception, just warning
   *
   * @see #fromClassPath(ClassLoader)
   * @param classLoader
   * @return tryFromClassPath
   */
  public static Stream<ClassPathResource> tryFromClassPath(ClassLoader classLoader) {
    try {
      return fromClassPath(classLoader);
    } catch (IOException e) {
      logger.log(Level.WARNING, e,
          () -> String.format("Can not find resource from class loader %s", classLoader));
    }
    return Stream.empty();
  }

  /**
   * Not throw IO exception, just warning
   *
   * @see #fromClassPath(ClassLoader, String)
   * @param classLoader
   * @param classPath
   * @return tryFromClassPath
   */
  public static Stream<ClassPathResource> tryFromClassPath(ClassLoader classLoader,
      String classPath) {
    try {
      return fromClassPath(classLoader, classPath);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> String
          .format("Can not find resource from class loader %s, path %s", classLoader, classPath));
    }
    return Stream.empty();
  }

  /**
   * Not throw IO exception, just warning
   *
   * @see #fromClassPath(String)
   * @param classPath
   * @return tryFromClassPath
   */
  public static Stream<ClassPathResource> tryFromClassPath(String classPath) {
    try {
      return fromClassPath(classPath);
    } catch (IOException e) {
      logger.log(Level.WARNING, e,
          () -> String.format("Can not find resource from path %s", classPath));
    }
    return Stream.empty();
  }

  /**
   * Not throw IO exception, just warning
   *
   * @see #fromFileSystem(Path)
   * @param path
   * @return tryFromFileSystem
   */
  public static FileSystemResource tryFromFileSystem(Path path) {
    try {
      return fromFileSystem(path);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> String.format("Can not find resource from path %s", path));
    }
    return null;
  }

  /**
   * Not throw IO exception, just warning
   *
   * @see #fromFileSystem(String)
   * @param path
   * @return tryFromFileSystem
   */
  public static Stream<FileSystemResource> tryFromFileSystem(String path) {
    try {
      return fromFileSystem(path);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> String.format("Can not find resource from path %s", path));
    }
    return null;
  }

  /**
   * Not throw IO exception, just warning
   *
   * @see #fromInputStream(InputStream, String)
   * @param inputStream
   * @param location
   * @return tryFromInputStream
   */
  public static InputStreamResource tryFromInputStream(InputStream inputStream, String location) {
    try {
      return fromInputStream(inputStream, location);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> "Can not find resource from input stream");
    }
    return null;
  }

  /**
   * Not throw IO exception, just warning
   *
   * @see #fromUrl(String)
   * @param url
   * @return tryFromUrl
   */
  public static URLResource tryFromUrl(String url) {
    try {
      return fromUrl(url);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> String.format("Can not find url resource from %s", url));
    }
    return null;
  }

  /**
   * Not throw IO exception, just warning
   *
   * @see #fromUrl(URL)
   * @param url
   * @return tryFromUrl
   */
  public static URLResource tryFromUrl(URL url) {
    try {
      return fromUrl(url);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> String.format("Can not find url resource from %s", url));
    }
    return null;
  }

  /**
   * Not throw IO exception, just warning
   *
   * @see #fromUrl(URL, Proxy)
   * @param url
   * @param proxy
   * @return tryFromUrl
   */
  public static InputStreamResource tryFromUrl(URL url, Proxy proxy) {
    try {
      return fromUrl(url, proxy);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> String.format("Can not find url resource from %s", url));
    }
    return null;
  }

  /**
   * corant-shared
   *
   * Describe class path resource include class resource.
   *
   * @author bingo 下午2:04:58
   *
   */
  public static class ClassPathResource extends URLResource {

    final ClassLoader classLoader;
    final String classPath;

    public ClassPathResource(String classPath, ClassLoader classLoader) {
      super(SourceType.CLASS_PATH);
      this.classLoader = shouldNotNull(classLoader);
      this.classPath = shouldNotNull(classPath);
    }

    public static ClassPathResource of(String classPath, ClassLoader classLoader) {
      if (classPath.endsWith(ClassUtils.CLASS_FILE_NAME_EXTENSION)) {
        return new ClassResource(classPath, classLoader);
      } else {
        return new ClassPathResource(classPath, classLoader);
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!super.equals(obj)) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ClassPathResource other = (ClassPathResource) obj;
      if (classLoader == null) {
        if (other.classLoader != null) {
          return false;
        }
      } else if (!classLoader.equals(other.classLoader)) {
        return false;
      }
      if (classPath == null) {
        if (other.classPath != null) {
          return false;
        }
      } else if (!classPath.equals(other.classPath)) {
        return false;
      }
      return true;
    }

    public ClassLoader getClassLoader() {
      return classLoader;
    }

    public String getClassPath() {
      return classPath;
    }

    @Override
    public String getLocation() {
      return classPath;
    }

    @Override
    public final URL getURL() {
      if (url == null) {
        synchronized (this) {
          if (url == null) {
            url = classLoader.getResource(classPath);
          }
        }
      }
      return url;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + (classLoader == null ? 0 : classLoader.hashCode());
      result = prime * result + (classPath == null ? 0 : classPath.hashCode());
      return result;
    }

    @Override
    public final InputStream openStream() throws IOException {
      URLConnection conn = getURL().openConnection();
      if (System.getProperty("os.name").toLowerCase(Locale.getDefault()).startsWith("window")) {
        conn.setUseCaches(false);
      }
      return conn.getInputStream();
    }

  }

  /**
   * corant-shared
   *
   * Describe class resource, but doesn't load it right away.
   *
   * @author bingo 下午2:04:09
   *
   */
  public static class ClassResource extends ClassPathResource {

    final String className;

    public ClassResource(String classPath, ClassLoader classLoader) {
      super(classPath, classLoader);
      int classNameEnd = classPath.length() - ClassUtils.CLASS_FILE_NAME_EXTENSION.length();
      className = classPath.substring(0, classNameEnd).replace(ClassPaths.PATH_SEPARATOR,
          ClassUtils.PACKAGE_SEPARATOR_CHAR);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!super.equals(obj)) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ClassResource other = (ClassResource) obj;
      if (className == null) {
        if (other.className != null) {
          return false;
        }
      } else if (!className.equals(other.className)) {
        return false;
      }
      return true;
    }

    public String getClassName() {
      return className;
    }

    public String getPackageName() {
      return ClassUtils.getPackageName(className);
    }

    public String getSimpleName() {
      return ClassUtils.getShortClassName(className);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + (className == null ? 0 : className.hashCode());
      return result;
    }

    public Class<?> load() {
      try {
        return classLoader.loadClass(className);
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException(e);
      }
    }

  }

  /**
   * corant-shared
   *
   * Describe system file
   *
   * @author bingo 下午6:53:19
   *
   */
  public static class FileSystemResource extends URLResource {

    final File file;

    public FileSystemResource(File file) {
      super(SourceType.FILE_SYSTEM);
      this.file = shouldNotNull(file);
      try {
        url = file.toURI().toURL();
      } catch (MalformedURLException e) {
        throw new CorantRuntimeException(e);
      }
    }

    public FileSystemResource(String location) {
      this(new File(shouldNotNull(location)));
    }

    public File getFile() {
      return file;
    }

    @Override
    public String getLocation() {
      return getFile().getAbsolutePath();
    }

    @Override
    public Map<String, Object> getMetadata() {
      return immutableMapOf("location", getLocation(), "sourceType", getSourceType(), "path",
          getFile().getPath(), "fileName", getName(), "lastModified", getFile().lastModified(),
          "length", getFile().length());
    }

    @Override
    public String getName() {
      return file.getName();
    }

    @Override
    public InputStream openStream() throws IOException {
      return new FileInputStream(file);
    }
  }

  /**
   * corant-shared
   *
   * Describe input stream resource, can specify a specific name.
   *
   * @author bingo 下午6:54:04
   *
   */
  public static class InputStreamResource implements Resource {
    final String name;
    final String location;
    final SourceType sourceType = SourceType.UNKNOWN;
    final InputStream inputStream;
    final URL url;

    /**
     * @param inputStream
     * @param name
     */
    public InputStreamResource(InputStream inputStream, String name) {
      super();
      this.name = name;
      this.inputStream = inputStream;
      location = null;
      url = null;
    }

    /**
     * @param location
     * @param inputStream
     */
    public InputStreamResource(String location, InputStream inputStream) {
      super();
      this.location = location;
      this.inputStream = inputStream;
      url = null;
      name = null;
    }

    /**
     *
     * @param url
     * @throws MalformedURLException
     * @throws IOException
     */
    public InputStreamResource(URL url) throws MalformedURLException, IOException {
      location = url.toExternalForm();
      inputStream = url.openStream();
      name = url.getFile();
      this.url = url;
    }

    @Override
    public String getLocation() {
      return location;
    }

    @Override
    public SourceType getSourceType() {
      return sourceType;
    }

    public URL getUrl() {
      return url;
    }

    @Override
    public InputStream openStream() throws IOException {
      return inputStream;
    }

  }

  /**
   * corant-shared
   *
   * <p>
   * Object that representation of a resource that can be loaded from URL, class, file system or
   * inputstream.
   * </p>
   *
   * @author bingo 下午3:19:30
   *
   */
  public interface Resource {

    /**
     * Location of this resource, depends on original source. Depending on source type, this may be:
     * <ul>
     * <li>FILE_SYSTEM - absolute path to the file</li>
     * <li>CLASS_PATH - class resource path</li>
     * <li>URL - string of the URI</li>
     * <li>UNKNOWN - whatever location was provided to {@link InputStreamResource}</li>
     * </ul>
     */
    String getLocation();

    /**
     * The informantions of this resource. For example, author, date created and date modified, size
     * etc.
     *
     * @return getMetadata
     */
    default Map<String, Object> getMetadata() {
      return immutableMapOf("location", getLocation(), "sourceType", getSourceType());
    }

    /**
     * The name of this resource. this may be:
     * <ul>
     * <li>FILE_SYSTEM - the underlying file name</li>
     * <li>CLASS_PATH - the underlying class path resource name</li>
     * <li>URL - the file name of this URL {@link URL#getFile()}</li>
     * <li>UNKNOWN - whatever name was provided to {@link InputStreamResource}</li>
     * </ul>
     *
     * @return getName
     */
    default String getName() {
      return null;
    }

    /**
     * original source type
     *
     * @return getSourceType
     */
    SourceType getSourceType();

    /**
     * Return an {@link InputStream} for the content of an resource
     *
     * @return
     * @throws IOException openStream
     */
    InputStream openStream() throws IOException;
  }

  /**
   * corant-shared
   *
   * <p>
   * Object that representation of a original source of a Resource
   * </p>
   *
   * @author bingo 下午3:31:29
   *
   */
  public enum SourceType {

    /**
     * load resource from file system
     */
    FILE_SYSTEM("filesystem:"),

    /**
     * load resource from class path
     */
    CLASS_PATH("classpath:"),

    /**
     * load resource from URL
     */
    URL("url:"),

    /**
     * load resource from input stream
     */
    UNKNOWN("unknown:");

    private final String prefix;
    private final int prefixLength;

    private SourceType(String prefix) {
      this.prefix = prefix;
      prefixLength = prefix.length();
    }

    public static Optional<SourceType> decide(String path) {
      SourceType ps = null;
      if (isNotBlank(path)) {
        for (SourceType p : SourceType.values()) {
          if (p.match(path) && path.length() > p.getPrefixLength()) {
            ps = p;
            break;
          }
        }
      }
      return Optional.ofNullable(ps);
    }

    public static String decideSeparator(String path) {
      return decide(path).orElse(UNKNOWN).getSeparator();
    }

    public String getPrefix() {
      return prefix;
    }

    public int getPrefixLength() {
      return prefixLength;
    }

    public String getSeparator() {
      if (this == CLASS_PATH || this == URL) {
        return "/";
      } else if (this == FILE_SYSTEM) {
        return File.separator;
      } else {
        return "";
      }
    }

    public boolean match(String path) {
      if (isBlank(path)) {
        return false;
      }
      return path.startsWith(prefix);
    }

    public String regulate(String path) {
      if (path != null && !path.startsWith(getPrefix())) {
        return getPrefix() + path;
      }
      return path;
    }

    public String resolve(String path) {
      if (path != null && path.startsWith(getPrefix())) {
        return path.substring(getPrefixLength());
      }
      return path;
    }
  }

  /**
   * corant-shared
   * <p>
   * A representation of a resource that load from URL
   *
   * @author bingo 下午4:06:15
   *
   */
  public static class URLResource implements Resource {
    final SourceType sourceType;
    volatile URL url;

    public URLResource(String url) throws MalformedURLException {
      this(new URL(url), SourceType.URL);
    }

    public URLResource(URL url) {
      this(url, SourceType.URL);
    }

    protected URLResource(SourceType sourceType) {
      this.sourceType = shouldNotNull(sourceType);
    }

    URLResource(URL url, SourceType sourceType) {
      this.url = shouldNotNull(url);
      this.sourceType = shouldNotNull(sourceType);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      URLResource other = (URLResource) obj;
      if (url == null) {
        if (other.url != null) {
          return false;
        }
      } else if (!url.equals(other.url)) {
        return false;
      }
      return true;
    }

    @Override
    public String getLocation() {
      return getURL().toExternalForm();
    }

    @Override
    public Map<String, Object> getMetadata() {
      return immutableMapOf("location", getLocation(), "sourceType", getSourceType(), "url",
          url.toExternalForm());
    }

    @Override
    public String getName() {
      return FileUtils.getFileName(getURL().getPath());
    }

    @Override
    public SourceType getSourceType() {
      return sourceType;
    }

    public URL getURL() {
      return url;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (getURL() == null ? 0 : getURL().hashCode());
      return result;
    }

    @Override
    public InputStream openStream() throws IOException {
      return getURL().openStream();
    }

  }
}

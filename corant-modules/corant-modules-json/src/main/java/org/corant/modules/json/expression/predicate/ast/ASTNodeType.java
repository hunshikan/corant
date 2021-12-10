/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.json.expression.predicate.ast;

import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.strip;
import org.corant.modules.json.expression.predicate.ast.ASTComparisonNode.ASTEqualNode;
import org.corant.modules.json.expression.predicate.ast.ASTComparisonNode.ASTGreaterEqualThanNode;
import org.corant.modules.json.expression.predicate.ast.ASTComparisonNode.ASTGreaterThanNode;
import org.corant.modules.json.expression.predicate.ast.ASTComparisonNode.ASTInNode;
import org.corant.modules.json.expression.predicate.ast.ASTComparisonNode.ASTLessEqualThanNode;
import org.corant.modules.json.expression.predicate.ast.ASTComparisonNode.ASTLessThanNode;
import org.corant.modules.json.expression.predicate.ast.ASTComparisonNode.ASTNoEqualNode;
import org.corant.modules.json.expression.predicate.ast.ASTComparisonNode.ASTNoInNode;
import org.corant.modules.json.expression.predicate.ast.ASTFunctionNode.ASTDefaultFunctionNode;
import org.corant.modules.json.expression.predicate.ast.ASTLogicNode.ASTLogicAndNode;
import org.corant.modules.json.expression.predicate.ast.ASTLogicNode.ASTLogicNorNode;
import org.corant.modules.json.expression.predicate.ast.ASTLogicNode.ASTLogicNotNode;
import org.corant.modules.json.expression.predicate.ast.ASTLogicNode.ASTLogicOrNode;
import org.corant.modules.json.expression.predicate.ast.ASTVariableNode.ASTDefaultVariableNode;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-modules-json
 *
 * @author bingo 下午5:13:27
 *
 */
public enum ASTNodeType {

  /**
   * Performs an AND operation on an array with at least two expressions and returns the objects
   * that meets all the expressions.
   */
  LG_AND("$and", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTLogicAndNode();
    }
  },

  /**
   * Performs a NOT operation on the specified expression and returns the objects that do not meet
   * the expression.
   */
  LG_NOT("$not", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTLogicNotNode();
    }
  },

  /**
   * Performs an OR operation on an array with at least two expressions and returns the objects that
   * meet at least one of the expressions.
   */
  LG_OR("$or", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTLogicOrNode();
    }
  },

  /**
   * Performs a NOR operation on an array with at least two expressions and returns the objects that
   * do not meet any of the expressions.
   */
  LG_NOR("$nor", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTLogicNorNode();
    }
  },

  /**
   * The equality comparator operator.
   */
  CP_EQ("$eq", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTEqualNode();
    }
  },

  /**
   * The inequality comparator operator.
   */
  CP_NE("$neq", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTNoEqualNode();
    }
  },

  /**
   * The in comparison operator. Use only one data type in the specified values.
   */
  CP_IN("$in", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTInNode();
    }
  },

  /**
   * The not in comparison operator.
   */
  CP_NIN("$nin", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTNoInNode();
    }
  },

  /**
   * The not in comparison operator.
   */
  CP_LT("$lt", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTLessThanNode();
    }
  },

  /**
   * The less than or equals comparison operator.
   */
  CP_LTE("$lte", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTLessEqualThanNode();
    }
  },

  /**
   * The greater than comparison operator.
   */
  CP_GT("$gt", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTGreaterThanNode();
    }
  },

  /**
   * The greater than or equals comparison operator.
   */
  CP_GTE("$gte", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTGreaterEqualThanNode();
    }
  },

  /**
   * The element match operator.
   */
  CP_EM("$element", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      throw new NotSupportedException();
    }
  },

  /**
   * The regular expression predicate.
   */
  CP_REGEX("$regex", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTGreaterEqualThanNode();
    }
  },

  /**
   * The variable node
   */
  VAR("#", true) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTDefaultVariableNode(object.toString().substring(1));
    }
  },

  VAL("", true) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTValueNode(object);
    }
  },

  /**
   * The function node
   */
  FUN("$fn:", false) {
    @Override
    public ASTNode<?> buildNode(Object object) {
      return new ASTDefaultFunctionNode(object.toString().substring(4));
    }
  };

  final String token;
  final boolean leaf;

  ASTNodeType(String token, boolean leaf) {
    this.token = token;
    this.leaf = leaf;
  }

  public static ASTNode<?> decideNode(Object object) {
    if (object instanceof String) {
      String useToken;
      if (!isBlank(useToken = strip((String) object))) {
        if (useToken.startsWith("#") && useToken.length() > 1) {
          return VAR.buildNode(useToken);
        } else if (useToken.startsWith(FUN.token) && useToken.length() > 4) {
          return FUN.buildNode(useToken);
        } else {
          for (ASTNodeType t : ASTNodeType.values()) {
            if (t.getToken().equalsIgnoreCase(useToken)) {
              return t.buildNode(useToken);
            }
          }
        }
      }
    }
    return ASTNodeType.VAL.buildNode(object);
  }

  public abstract ASTNode<?> buildNode(Object object);

  public String getToken() {
    return token;
  }

  public boolean isLeaf() {
    return leaf;
  }

}

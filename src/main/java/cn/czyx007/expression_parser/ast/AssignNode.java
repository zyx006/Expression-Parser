package cn.czyx007.expression_parser.ast;

import java.util.Map;

/**
 * 赋值节点 - 将表达式的值赋给变量
 */
public class AssignNode extends ExprNode {
    private final String varName;
    private final ExprNode valueExpr;

    public AssignNode(String varName, ExprNode valueExpr) {
        this.varName = varName;
        this.valueExpr = valueExpr;
    }

    public String getVarName() {
        return varName;
    }

    @Override
    public double eval(Map<String, Double> context) {
        double value = valueExpr.eval(context);
        if (context != null) {
            context.put(varName, value);
        }
        return value;
    }
}


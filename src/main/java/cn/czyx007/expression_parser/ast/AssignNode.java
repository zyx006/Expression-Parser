package cn.czyx007.expression_parser.ast;

import java.util.Map;

/**
 * 赋值节点<br/>
 * 将表达式的值赋给变量，支持标量和数组赋值
 */
public class AssignNode extends ExprNode {
    private final String varName;
    private final ExprNode valueExpr;

    /**
     * 构造赋值节点
     * @param varName 变量名
     * @param valueExpr 值表达式
     */
    public AssignNode(String varName, ExprNode valueExpr) {
        this.varName = varName;
        this.valueExpr = valueExpr;
    }

    /**
     * 获取变量名
     * @return 变量名
     */
    public String getVarName() {
        return varName;
    }

    /**
     * 获取值表达式
     * @return 值表达式节点
     */
    public ExprNode getValueExpr() {
        return valueExpr;
    }

    @Override
    public double eval(Map<String, Double> context) {
        double value = valueExpr.eval(context);
        if (context != null) {
            context.put(varName, value);
        }
        return value;
    }

    @Override
    public Value evalValue(Map<String, Object> context) {
        Value value = valueExpr.evalValue(context);
        if (context != null) {
            context.put(varName, value);
        }
        return value;
    }

    @Override
    public boolean isArrayExpression() {
        return valueExpr.isArrayExpression();
    }
}

package cn.czyx007.expression_parser.ast;

import cn.czyx007.expression_parser.exception.ErrorCode;
import cn.czyx007.expression_parser.exception.ExpressionException;

import java.util.Map;

/**
 * 变量节点<br/>
 * 从上下文中读取变量值，支持标量和数组变量
 */
public class VariableNode extends ExprNode {
    private final String name;

    /**
     * 构造变量节点
     * @param name 变量名
     */
    public VariableNode(String name) {
        this.name = name;
    }

    /**
     * 获取变量名
     * @return 变量名
     */
    public String getName() {
        return name;
    }

    @Override
    public double eval(Map<String, Double> context) {
        if (context == null || !context.containsKey(name)) {
            throw new ExpressionException(ErrorCode.UNDEFINED_VARIABLE, name);
        }
        return context.get(name);
    }

    @Override
    public Value evalValue(Map<String, Object> context) {
        if (context == null || !context.containsKey(name)) {
            throw new ExpressionException(ErrorCode.UNDEFINED_VARIABLE, name);
        }
        Object val = context.get(name);
        if (val instanceof Value) {
            return (Value) val;
        } else if (val instanceof Double) {
            return new Value((Double) val);
        }
        throw new ExpressionException(ErrorCode.INVALID_VARIABLE_TYPE, name);
    }
}

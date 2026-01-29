package cn.czyx007.expression_parser.ast;

import java.util.Map;

/**
 * 变量节点 - 从上下文中读取变量值
 */
public class VariableNode extends ExprNode {
    private final String name;

    public VariableNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public double eval(Map<String, Double> context) {
        if (context == null || !context.containsKey(name)) {
            throw new RuntimeException("未定义的变量: " + name);
        }
        return context.get(name);
    }

    @Override
    public Value evalValue(Map<String, Object> context) {
        if (context == null || !context.containsKey(name)) {
            throw new RuntimeException("未定义的变量: " + name);
        }
        Object val = context.get(name);
        if (val instanceof Value) {
            return (Value) val;
        } else if (val instanceof Double) {
            return new Value((Double) val);
        }
        throw new RuntimeException("变量 " + name + " 的值类型不正确");
    }
}

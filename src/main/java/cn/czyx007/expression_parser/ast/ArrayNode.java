package cn.czyx007.expression_parser.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数组节点 - 表示数组字面量如 [1, 2, 3] 或 [[1,2], [3,4]]
 */
public class ArrayNode extends ExprNode {
    private final List<ExprNode> elements;

    public ArrayNode(List<ExprNode> elements) {
        this.elements = elements;
    }

    public List<ExprNode> getElements() {
        return elements;
    }

    @Override
    public double eval(Map<String, Double> context) {
        throw new RuntimeException("数组不能直接求值为标量，请使用 evalValue 方法");
    }

    @Override
    public Value evalValue(Map<String, Object> context) {
        List<Value> values = new ArrayList<>();
        for (ExprNode elem : elements) {
            values.add(elem.evalValue(context));
        }
        return new Value(values);
    }

    @Override
    public boolean isArrayExpression() {
        return true;
    }
}

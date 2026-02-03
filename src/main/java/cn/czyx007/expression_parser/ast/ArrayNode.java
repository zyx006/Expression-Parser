package cn.czyx007.expression_parser.ast;

import cn.czyx007.expression_parser.exception.ErrorCode;
import cn.czyx007.expression_parser.exception.ExpressionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数组节点<br/>
 * 表示数组字面量，如 [1, 2, 3] 或 [[1,2], [3,4]]
 */
public class ArrayNode extends ExprNode {
    private final List<ExprNode> elements;

    /**
     * 构造数组节点
     * @param elements 数组元素列表
     */
    public ArrayNode(List<ExprNode> elements) {
        this.elements = elements;
    }

    /**
     * 获取数组元素列表
     * @return 元素列表
     */
    public List<ExprNode> getElements() {
        return elements;
    }

    @Override
    public double eval(Map<String, Double> context) {
        throw new ExpressionException(ErrorCode.ARRAY_CANNOT_EVAL_AS_SCALAR);
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

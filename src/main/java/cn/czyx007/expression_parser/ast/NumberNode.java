package cn.czyx007.expression_parser.ast;

import java.util.Map;

/**
 * 数字节点<br/>
 * 表示数字常量（整数或浮点数）
 */
public class NumberNode extends ExprNode {
    private final double value;

    /**
     * 构造数字节点
     * @param value 数字值
     */
    public NumberNode(double value) {
        this.value = value;
    }

    @Override
    public double eval(Map<String, Double> context) {
        return value;
    }
}


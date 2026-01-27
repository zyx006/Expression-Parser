package cn.czyx007.expression_parser.ast;

import java.util.Map;

public class NumberNode extends ExprNode {
    private final double value;

    public NumberNode(double value) {
        this.value = value;
    }

    @Override
    public double eval(Map<String, Double> context) {
        return value;
    }
}


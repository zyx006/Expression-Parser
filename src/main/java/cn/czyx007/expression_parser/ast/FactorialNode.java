package cn.czyx007.expression_parser.ast;

import cn.czyx007.expression_parser.exception.ErrorCode;
import cn.czyx007.expression_parser.exception.ExpressionException;

import java.util.Map;

/**
 * 阶乘节点 - 计算 n!
 */
public class FactorialNode extends ExprNode {
    private final ExprNode expr;

    public FactorialNode(ExprNode expr) {
        this.expr = expr;
    }

    @Override
    public double eval(Map<String, Double> context) {
        double val = expr.eval(context);
        return eval(val);
    }

    @Override
    public Value evalValue(Map<String, Object> context) {
        Value val = expr.evalValue(context);
        if (!val.isScalar()) {
            throw new ExpressionException(ErrorCode.ARRAY_NOT_SUPPORTED_FACTORIAL);
        }
        return new Value(eval(val.asScalar()));
    }

    // 核心计算逻辑
    private double eval(double val) {
        // 阶乘要求非负整数
        if (val < 0 || val != Math.floor(val)) {
            throw new ExpressionException(ErrorCode.FACTORIAL_NEGATIVE, val);
        }
        int n = (int) val;
        if (n > 170) {
            throw new ExpressionException(ErrorCode.FACTORIAL_TOO_LARGE);
        }
        return factorial(n);
    }

    private double factorial(int n) {
        if (n <= 1) return 1.0;
        double result = 1.0;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }
}


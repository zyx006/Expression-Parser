package cn.czyx007.expression_parser.ast;

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
        // 阶乘要求非负整数
        if (val < 0 || val != Math.floor(val)) {
            throw new ArithmeticException("阶乘要求非负整数，但得到 " + val);
        }
        int n = (int) val;
        if (n > 170) {
            throw new ArithmeticException("阶乘参数过大（最大支持 170!）");
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


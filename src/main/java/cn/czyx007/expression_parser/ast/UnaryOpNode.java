package cn.czyx007.expression_parser.ast;

import cn.czyx007.expression_parser.lexer.Token;
import cn.czyx007.expression_parser.lexer.TokenType;

import java.util.Map;

// 一元运算节点 (用于处理 -5, +5)
public class UnaryOpNode extends ExprNode {
    private final ExprNode expr;
    private final Token op;

    public UnaryOpNode(Token op, ExprNode expr) {
        this.op = op;
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
            throw new RuntimeException("一元操作符 '" + op.value() + "' 不支持数组操作数");
        }
        return new Value(eval(val.asScalar()));
    }

    // 核心计算逻辑
    private double eval(double val) {
        if (op.type() == TokenType.PLUS) return +val;
        if (op.type() == TokenType.MINUS) return -val;
        throw new RuntimeException("未知一元操作符: " + op.type());
    }
}


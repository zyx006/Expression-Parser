package cn.czyx007.expression_parser.ast;

import cn.czyx007.expression_parser.exception.ErrorCode;
import cn.czyx007.expression_parser.exception.ExpressionException;
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
            throw new ExpressionException(ErrorCode.ARRAY_NOT_SUPPORTED_UNARY, op.value());
        }
        return new Value(eval(val.asScalar()));
    }

    // 核心计算逻辑
    private double eval(double val) {
        if (op.type() == TokenType.PLUS) return +val;
        if (op.type() == TokenType.MINUS) return -val;
        throw new ExpressionException(ErrorCode.UNKNOWN_UNARY_OPERATOR, op.type());
    }
}


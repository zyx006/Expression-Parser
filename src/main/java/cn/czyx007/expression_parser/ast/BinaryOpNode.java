package cn.czyx007.expression_parser.ast;

import cn.czyx007.expression_parser.lexer.Token;

import java.util.Map;

public class BinaryOpNode extends ExprNode {
    private final ExprNode left;
    private final ExprNode right;
    private final Token op;

    public BinaryOpNode(ExprNode left, Token op, ExprNode right) {
        this.left = left;
        this.right = right;
        this.op = op;
    }

    @Override
    public double eval(Map<String, Double> context) {
        double leftVal = left.eval(context);
        double rightVal = right.eval(context);

        double result;
        switch (op.type()) {
            case PLUS: result = leftVal + rightVal; break;
            case MINUS: result = leftVal - rightVal; break;
            case MULTIPLY: result = leftVal * rightVal; break;
            case DIVIDE:
                if (rightVal == 0) throw new ArithmeticException("除数不能为0");
                result = leftVal / rightVal; break;
            case MODULO:
                if (rightVal == 0) throw new ArithmeticException("模数不能为0");
                result = leftVal % rightVal; break;
            case POWER:
                result = Math.pow(leftVal, rightVal); break;
            default: throw new RuntimeException("未知的操作符: " + op.type());
        }
        return fixPrecision(result);
    }
}


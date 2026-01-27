package cn.czyx007.expression_parser.ast;

import java.util.List;
import java.util.Map;

/**
 * 语句列表节点 - 用于支持分号分隔的多语句表达式
 * 如 "x=10; y=2x; x+y"
 * 求值时依次执行所有语句，返回最后一个语句的值
 */
public class StatementListNode extends ExprNode {
    private final List<ExprNode> statements;

    public StatementListNode(List<ExprNode> statements) {
        this.statements = statements;
    }

    @Override
    public double eval(Map<String, Double> context) {
        double result = 0;
        for (ExprNode stmt : statements) {
            result = stmt.eval(context);
        }
        return result;
    }
}


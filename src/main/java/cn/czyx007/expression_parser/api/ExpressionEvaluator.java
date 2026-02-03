package cn.czyx007.expression_parser.api;

import cn.czyx007.expression_parser.ast.ExprNode;
import cn.czyx007.expression_parser.ast.Value;
import cn.czyx007.expression_parser.lexer.Lexer;
import cn.czyx007.expression_parser.parser.Parser;

import java.util.HashMap;
import java.util.Map;

/**
 * 表达式求值器 - 对外 API <br/>
 * 提供简洁的表达式计算接口
 */
public class ExpressionEvaluator {

    /**
     * 计算表达式（全新上下文）
     * @param expression 表达式字符串
     * @return 计算结果
     */
    public static Value eval(String expression) {
        return eval(expression, new HashMap<>());
    }

    /**
     * 计算表达式（使用指定上下文）
     * @param expression 表达式字符串
     * @param context    变量上下文
     * @return 计算结果
     */
    public static Value eval(String expression, Map<String, Object> context) {
        Lexer lexer = new Lexer(expression);
        Parser parser = new Parser(lexer);
        ExprNode ast = parser.parse();
        return ast.evalValue(context);
    }
}

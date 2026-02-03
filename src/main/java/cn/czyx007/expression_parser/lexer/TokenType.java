package cn.czyx007.expression_parser.lexer;

/**
 * Token 类型枚举 <br/>
 * 定义词法分析器可识别的所有标记类型
 */
public enum TokenType {
    /** 数字（含浮点数、科学计数法） */
    NUMBER,
    /** 标识符（变量名、函数名、常量名） */
    IDENTIFIER,
    /** 加号 + */
    PLUS,
    /** 减号 - */
    MINUS,
    /** 乘号 * */
    MULTIPLY,
    /** 除号 / */
    DIVIDE,
    /** 取模 % */
    MODULO,
    /** 幂运算 ^ */
    POWER,
    /** 阶乘 ! */
    FACTORIAL,
    /** 赋值 = */
    ASSIGN,
    /** 逗号 ,（用于多参数函数） */
    COMMA,
    /** 分号 ;（用于分隔多语句） */
    SEMICOLON,
    /** 左括号 ( */
    LPAREN,
    /** 右括号 ) */
    RPAREN,
    /** 左方括号 [（用于数组） */
    LBRACKET,
    /** 右方括号 ]（用于数组） */
    RBRACKET,
    /** 表达式结束标记 */
    EOF
}

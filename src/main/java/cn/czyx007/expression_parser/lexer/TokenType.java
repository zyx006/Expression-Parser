package cn.czyx007.expression_parser.lexer;

// 定义 Token 类型
public enum TokenType {
    NUMBER,         // 数字（含浮点数、科学计数法）
    IDENTIFIER,     // 标识符（变量名、函数名、常量名）
    PLUS, MINUS, MULTIPLY, DIVIDE, MODULO, POWER,
    FACTORIAL,      // 阶乘 !
    ASSIGN,         // 赋值 =
    COMMA,          // 逗号 , (用于多参数函数)
    SEMICOLON,      // 分号 ; (用于分隔多语句)
    LPAREN, RPAREN,
    LBRACKET, RBRACKET,  // 方括号 [] (用于数组)
    EOF
}

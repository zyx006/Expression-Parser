package cn.czyx007.expression_parser.lexer;

// Token 类
public class Token {
    private final TokenType type;
    private final String value;
    private final int position;

    public Token(TokenType type, String value, int position) {
        this.type = type;
        this.value = value;
        this.position = position;
    }

    public TokenType type() {
        return type;
    }

    public String value() {
        return value;
    }

    public int position() {
        return position;
    }

    @Override
    public String toString() {
        return "Token{" + type + "='" + value + "' @" + position + "}";
    }
}


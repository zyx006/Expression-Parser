package cn.czyx007.expression_parser.lexer;

/**
 * 词法标记（Token） <br/>
 * 表示词法分析过程中识别出的最小语法单元
 */
public class Token {
    private final TokenType type;
    private final String value;
    private final int position;

    /**
     * 构造一个 Token
     * @param type     标记类型
     * @param value    标记的字符串值
     * @param position 标记在源字符串中的起始位置
     */
    public Token(TokenType type, String value, int position) {
        this.type = type;
        this.value = value;
        this.position = position;
    }

    /**
     * 获取标记类型
     * @return 标记类型
     */
    public TokenType type() {
        return type;
    }

    /**
     * 获取标记的字符串值
     * @return 标记值
     */
    public String value() {
        return value;
    }

    /**
     * 获取标记在源字符串中的位置
     * @return 位置索引
     */
    public int position() {
        return position;
    }

    @Override
    public String toString() {
        return "Token{" + type + "='" + value + "' @" + position + "}";
    }
}


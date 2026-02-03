package cn.czyx007.expression_parser.lexer;

import cn.czyx007.expression_parser.exception.ErrorCode;
import cn.czyx007.expression_parser.exception.ExpressionException;

/**
 * 词法分析器（Lexer） <br/>
 * 将输入的表达式字符串分解为 Token 序列 <br/>
 * 支持数字、标识符、运算符、括号、数组字面量等
 */
public class Lexer {
    private final String input;
    private int pos = 0;
    private char currentChar;

    // lookahead：缓存预读的 token
    private Token peekedToken = null;

    /**
     * 构造词法分析器
     * @param input 待解析的表达式字符串
     */
    public Lexer(String input) {
        this.input = input;
        if (input.length() > 0) {
            this.currentChar = input.charAt(0);
        } else {
            this.currentChar = '\0';
        }
    }

    /**
     * 预览下一个 token（不消耗）
     * @return 下一个 token
     */
    public Token peek() {
        if (peekedToken == null) {
            peekedToken = scanNextToken();
        }
        return peekedToken;
    }

    /**
     * 获取下一个 token（消耗）
     * @return 下一个 token
     */
    public Token getNextToken() {
        if (peekedToken != null) {
            Token token = peekedToken;
            peekedToken = null;
            return token;
        }
        return scanNextToken();
    }

    /**
     * 前进一个字符
     */
    private void advance() {
        pos++;
        if (pos > input.length() - 1) {
            currentChar = '\0';
        } else {
            currentChar = input.charAt(pos);
        }
    }

    /**
     * 跳过空白字符
     */
    private void skipWhitespace() {
        while (currentChar != '\0' && Character.isWhitespace(currentChar)) {
            advance();
        }
    }

    /**
     * 解析数字（支持浮点数、科学计数法） <br/>
     * 支持格式：整数、小数、科学计数法（如 1.23e-4, 1.5E10）
     * @return 数字的字符串表示
     */
    private String number() {
        StringBuilder result = new StringBuilder();
        // 处理整数部分
        while (currentChar != '\0' && Character.isDigit(currentChar)) {
            result.append(currentChar);
            advance();
        }
        // 处理小数部分
        if (currentChar == '.') {
            result.append(currentChar);
            advance();
            while (currentChar != '\0' && Character.isDigit(currentChar)) {
                result.append(currentChar);
                advance();
            }
        }
        // 科学计数法支持: e/E 后可跟 +/- 和数字
        if (currentChar == 'e' || currentChar == 'E') {
            result.append(currentChar);
            advance();
            if (currentChar == '+' || currentChar == '-') {
                result.append(currentChar);
                advance();
            }
            if (!Character.isDigit(currentChar)) {
                throw new ExpressionException(ErrorCode.INVALID_SCIENTIFIC_NOTATION, pos);
            }
            while (currentChar != '\0' && Character.isDigit(currentChar)) {
                result.append(currentChar);
                advance();
            }
        }
        return result.toString();
    }

    /**
     * 解析标识符（字母或下划线开头，可包含字母、数字和下划线） <br/>
     * 用于变量名、函数名、常量名
     * @return 标识符字符串
     */
    private String identifier() {
        StringBuilder result = new StringBuilder();
        while (currentChar != '\0' && (Character.isLetterOrDigit(currentChar) || currentChar == '_')) {
            result.append(currentChar);
            advance();
        }
        return result.toString();
    }

    /**
     * 扫描下一个 token <br/>
     * 内部方法，执行实际的词法分析
     * @return 识别出的 token
     */
    private Token scanNextToken() {
        while (currentChar != '\0') {
            if (Character.isWhitespace(currentChar)) {
                skipWhitespace();
                continue;
            }
            int tokenPos = pos; // 记录 token 起始位置

            // 标识符支持（变量名、函数名、常量名）
            if (Character.isLetter(currentChar) || currentChar == '_') {
                return new Token(TokenType.IDENTIFIER, identifier(), tokenPos);
            }

            // 支持 .5 这种省略前导零的数字
            if (Character.isDigit(currentChar) || (currentChar == '.' && pos + 1 < input.length() && Character.isDigit(input.charAt(pos + 1)))) {
                return new Token(TokenType.NUMBER, number(), tokenPos);
            }
            if (currentChar == '+') {
                advance();
                return new Token(TokenType.PLUS, "+", tokenPos);
            }
            if (currentChar == '-') {
                advance();
                return new Token(TokenType.MINUS, "-", tokenPos);
            }
            if (currentChar == '*') {
                advance();
                return new Token(TokenType.MULTIPLY, "*", tokenPos);
            }
            if (currentChar == '/') {
                advance();
                return new Token(TokenType.DIVIDE, "/", tokenPos);
            }
            if (currentChar == '%') {
                advance();
                return new Token(TokenType.MODULO, "%", tokenPos);
            }
            if (currentChar == '^') {
                advance();
                return new Token(TokenType.POWER, "^", tokenPos);
            }
            if (currentChar == '!') {
                advance();
                return new Token(TokenType.FACTORIAL, "!", tokenPos);
            }
            if (currentChar == '=') {
                advance();
                return new Token(TokenType.ASSIGN, "=", tokenPos);
            }
            if (currentChar == ',') {
                advance();
                return new Token(TokenType.COMMA, ",", tokenPos);
            }
            if (currentChar == ';') {
                advance();
                return new Token(TokenType.SEMICOLON, ";", tokenPos);
            }
            if (currentChar == '(') {
                advance();
                return new Token(TokenType.LPAREN, "(", tokenPos);
            }
            if (currentChar == ')') {
                advance();
                return new Token(TokenType.RPAREN, ")", tokenPos);
            }
            if (currentChar == '[') {
                advance();
                return new Token(TokenType.LBRACKET, "[", tokenPos);
            }
            if (currentChar == ']') {
                advance();
                return new Token(TokenType.RBRACKET, "]", tokenPos);
            }
            throw new ExpressionException(ErrorCode.ILLEGAL_CHARACTER, currentChar, tokenPos);
        }
        return new Token(TokenType.EOF, "", pos);
    }
}


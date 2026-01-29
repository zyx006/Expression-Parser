package cn.czyx007.expression_parser.lexer;

// 词法分析器
public class Lexer {
    private final String input;
    private int pos = 0;
    private char currentChar;

    // lookahead：缓存预读的 token
    private Token peekedToken = null;

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
     */
    public Token peek() {
        if (peekedToken == null) {
            peekedToken = scanNextToken();
        }
        return peekedToken;
    }

    /**
     * 获取下一个 token（消耗）
     */
    public Token getNextToken() {
        if (peekedToken != null) {
            Token token = peekedToken;
            peekedToken = null;
            return token;
        }
        return scanNextToken();
    }

    private void advance() {
        pos++;
        if (pos > input.length() - 1) {
            currentChar = '\0';
        } else {
            currentChar = input.charAt(pos);
        }
    }

    private void skipWhitespace() {
        while (currentChar != '\0' && Character.isWhitespace(currentChar)) {
            advance();
        }
    }

    // 支持 浮点数、科学计数法（如 1.23e-4, 1.5E10）
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
                throw new RuntimeException("位置 " + pos + ": 科学计数法格式错误，期望数字");
            }
            while (currentChar != '\0' && Character.isDigit(currentChar)) {
                result.append(currentChar);
                advance();
            }
        }
        return result.toString();
    }

    // 解析标识符（字母开头，可包含字母和数字）
    private String identifier() {
        StringBuilder result = new StringBuilder();
        while (currentChar != '\0' && (Character.isLetterOrDigit(currentChar) || currentChar == '_')) {
            result.append(currentChar);
            advance();
        }
        return result.toString();
    }

    /**
     * 内部扫描方法，实际执行词法分析
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
            throw new RuntimeException("位置 " + tokenPos + ": 非法字符 '" + currentChar + "'");
        }
        return new Token(TokenType.EOF, "", pos);
    }
}


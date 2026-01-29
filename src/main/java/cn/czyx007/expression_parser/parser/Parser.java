package cn.czyx007.expression_parser.parser;

import cn.czyx007.expression_parser.ast.*;
import cn.czyx007.expression_parser.lexer.Lexer;
import cn.czyx007.expression_parser.lexer.Token;
import cn.czyx007.expression_parser.lexer.TokenType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 语法解析器
public class Parser {
    private final Lexer lexer;
    private Token currentToken;

    // 预定义常量
    private static final Set<String> CONSTANTS = new HashSet<>();
    static {
        CONSTANTS.add("PI");
        CONSTANTS.add("E");
    }

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.currentToken = this.lexer.getNextToken();
    }

    private void eat(TokenType type) {
        if (currentToken.type() == type) {
            currentToken = lexer.getNextToken();
        } else {
            throw new RuntimeException("位置 " + currentToken.position() + ": 语法错误，期望 " + getTypeName(type) + "，但得到 " + getTypeName(currentToken.type()) + " '" + currentToken.value() + "'");
        }
    }

    private String getTypeName(TokenType type) {
        switch (type) {
            case NUMBER: return "数字";
            case IDENTIFIER: return "标识符";
            case PLUS: return "'+'";
            case MINUS: return "'-'";
            case MULTIPLY: return "'*'";
            case DIVIDE: return "'/'";
            case MODULO: return "'%'";
            case POWER: return "'^'";
            case FACTORIAL: return "'!'";
            case ASSIGN: return "'='";
            case COMMA: return "','";
            case SEMICOLON: return "';'";
            case LPAREN: return "'('";
            case RPAREN: return "')'";
            case LBRACKET: return "'['";
            case RBRACKET: return "']'";
            case EOF: return "表达式结束";
            default: return "未知类型";
        }
    }

    /**
     * factor : NUMBER
     *        | IDENTIFIER (函数调用/常量/变量)
     *        | LPAREN expr RPAREN
     *        | LBRACKET (expr (COMMA expr)*)? RBRACKET  (数组字面量)
     * 处理基础元素：数字、标识符、括号表达式、数组。
     */
    private ExprNode factor() {
        Token token = currentToken;

        if (token.type() == TokenType.NUMBER) {
            eat(TokenType.NUMBER);
            return new NumberNode(Double.parseDouble(token.value()));
        }
        else if (token.type() == TokenType.IDENTIFIER) {
            String name = token.value();
            eat(TokenType.IDENTIFIER);

            // 检查是否为常量 (PI, E)
            if (CONSTANTS.contains(name.toUpperCase())) {
                return getConstantNode(name.toUpperCase());
            }

            // 检查是否为函数调用 (后面跟着左括号)
            if (currentToken.type() == TokenType.LPAREN) {
                return parseFunctionCall(name);
            }

            // 否则为变量
            return new VariableNode(name);
        }
        else if (token.type() == TokenType.LPAREN) {
            eat(TokenType.LPAREN);
            ExprNode node = expr();
            eat(TokenType.RPAREN);
            return node;
        }
        else if (token.type() == TokenType.LBRACKET) {
            return parseArrayLiteral();
        }
        throw new RuntimeException("位置 " + token.position() + ": 语法错误，期望数字、标识符、左括号或左方括号，但得到 " + getTypeName(token.type()) + " '" + token.value() + "'");
    }

    /**
     * 解析数组字面量: LBRACKET (expr (COMMA expr)*)? RBRACKET
     * 支持 [1, 2, 3] 或 [[1,2], [3,4]] 等
     */
    private ExprNode parseArrayLiteral() {
        eat(TokenType.LBRACKET);
        List<ExprNode> elements = new ArrayList<>();

        if (currentToken.type() != TokenType.RBRACKET) {
            elements.add(expr());
            while (currentToken.type() == TokenType.COMMA) {
                eat(TokenType.COMMA);
                elements.add(expr());
            }
        }
        eat(TokenType.RBRACKET);
        return new ArrayNode(elements);
    }

    /**
     * 解析函数调用: IDENTIFIER LPAREN (expr (COMMA expr)*)? RPAREN
     */
    private ExprNode parseFunctionCall(String funcName) {
        eat(TokenType.LPAREN);
        List<ExprNode> args = new ArrayList<>();

        if (currentToken.type() != TokenType.RPAREN) {
            args.add(expr());
            while (currentToken.type() == TokenType.COMMA) {
                eat(TokenType.COMMA);
                args.add(expr());
            }
        }
        eat(TokenType.RPAREN);
        return new FunctionNode(funcName, args);
    }

    /**
     * 获取常量对应的节点
     */
    private ExprNode getConstantNode(String name) {
        switch (name) {
            case "PI": return new NumberNode(Math.PI);
            case "E": return new NumberNode(Math.E);
            default: throw new RuntimeException("未知常量: " + name);
        }
    }

    /**
     * postfix : factor ('!')*
     * 处理后缀运算符（阶乘）
     */
    private ExprNode postfix() {
        ExprNode node = factor();

        while (currentToken.type() == TokenType.FACTORIAL) {
            eat(TokenType.FACTORIAL);
            node = new FactorialNode(node);
        }
        return node;
    }

    /**
     * implicitMul : postfix (隐式乘法)*
     * 处理隐式乘法：当 postfix 后面紧跟 NUMBER, IDENTIFIER, 或 LPAREN 时，自动插入乘法
     * 例如: 2x -> 2*x, 3(4+5) -> 3*(4+5), 2PI -> 2*PI
     */
    private ExprNode implicitMul() {
        ExprNode node = postfix();

        while (isImplicitMulStart()) {
            // 隐式乘法
            ExprNode right = postfix();
            // 创建一个虚拟的乘法 Token
            Token mulToken = new Token(TokenType.MULTIPLY, "*", -1);
            node = new BinaryOpNode(node, mulToken, right);
        }
        return node;
    }

    /**
     * 判断当前 token 是否可以开始隐式乘法的右操作数
     * 注意：数字与数字之间不允许隐式乘法（如 "1 2" 是非法的）
     * 仅支持：数字/右括号 后跟 标识符/左括号 的情况
     */
    private boolean isImplicitMulStart() {
        TokenType type = currentToken.type();
        // 仅允许 IDENTIFIER 或 LPAREN 作为隐式乘法的右操作数
        // 不允许 NUMBER，即 "2 3" 不会被解析为 "2*3"
        return type == TokenType.IDENTIFIER || type == TokenType.LPAREN;
    }

    /**
     * power : implicitMul ( '^' power )?
     * 幂运算实现。
     * 1. 优先级：最高（仅次于括号和数字）。
     * 2. 结合性：右结合。
     *    如果遇到 '^'，右侧递归调用 power() 而非 implicitMul()。
     *    这样对于 "2^3^2"，解析树会变成 ^(2, ^(3, 2))，即 2^(3^2) = 512。
     */
    private ExprNode power() {
        ExprNode left = implicitMul();

        if (currentToken.type() == TokenType.POWER) {
            Token op = currentToken;
            eat(TokenType.POWER);
            // 递归调用 power() 实现右结合
            ExprNode right = power();
            return new BinaryOpNode(left, op, right);
        }

        return left;
    }

    /**
     * unary : (+|-) unary | power
     * 一元运算符优先级低于幂运算。
     * 这样 -3^2 会被解析为 -(3^2) = -9，符合数学惯例。
     * 优先级：幂运算 > 一元运算符 > 乘除模 > 加减
     */
    private ExprNode unary() {
        Token token = currentToken;
        if (token.type() == TokenType.PLUS || token.type() == TokenType.MINUS) {
            eat(token.type());
            // 递归调用 unary 以支持 --5 这样的写法
            return new UnaryOpNode(token, unary());
        }
        return power();
    }

    /**
     * term : unary ((MUL | DIV | MOD) unary)*
     * 处理乘、除、取模。
     * 调用 unary() 作为基本单元，确保幂运算和一元运算符优先于乘除。
     */
    private ExprNode term() {
        ExprNode node = unary();

        while (currentToken.type() == TokenType.MULTIPLY ||
                currentToken.type() == TokenType.DIVIDE ||
                currentToken.type() == TokenType.MODULO) {
            Token token = currentToken;
            if (token.type() == TokenType.MULTIPLY) {
                eat(TokenType.MULTIPLY);
            } else if (token.type() == TokenType.DIVIDE) {
                eat(TokenType.DIVIDE);
            } else if (token.type() == TokenType.MODULO) {
                eat(TokenType.MODULO);
            }
            // 乘除是左结合的，所以继续循环调用 unary()
            node = new BinaryOpNode(node, token, unary());
        }
        return node;
    }

    /**
     * addExpr : term ((PLUS | MINUS) term)*
     * 处理加法和减法。
     */
    private ExprNode addExpr() {
        ExprNode node = term();

        while (currentToken.type() == TokenType.PLUS || currentToken.type() == TokenType.MINUS) {
            Token token = currentToken;
            if (token.type() == TokenType.PLUS) {
                eat(TokenType.PLUS);
            } else if (token.type() == TokenType.MINUS) {
                eat(TokenType.MINUS);
            }
            node = new BinaryOpNode(node, token, term());
        }
        return node;
    }

    /**
     * expr : IDENTIFIER ASSIGN expr | addExpr
     * 处理赋值表达式（最低优先级）
     * 使用 lookahead 简化逻辑，避免复杂回溯
     */
    public ExprNode expr() {
        // 使用 lookahead 检查是否为赋值语句：IDENTIFIER = ...
        if (currentToken.type() == TokenType.IDENTIFIER && lexer.peek().type() == TokenType.ASSIGN) {
            Token idToken = currentToken;
            eat(TokenType.IDENTIFIER);
            eat(TokenType.ASSIGN);
            ExprNode valueExpr = expr(); // 右结合
            return new AssignNode(idToken.value(), valueExpr);
        }
        return addExpr();
    }


    /**
     * program : statementList
     * statementList : expr (SEMICOLON expr)* SEMICOLON?
     * 支持分号分隔的多语句表达式，如 "x=10; y=2x; x+y"
     * 返回最后一个表达式的结果
     */
    public ExprNode parse() {
        // 如果输入为空，直接返回
        if (currentToken.type() == TokenType.EOF) {
            throw new RuntimeException("表达式不能为空");
        }

        List<ExprNode> statements = new ArrayList<>();
        statements.add(expr());

        // 解析分号分隔的多个语句
        while (currentToken.type() == TokenType.SEMICOLON) {
            eat(TokenType.SEMICOLON);
            // 允许末尾分号（分号后面是 EOF）
            if (currentToken.type() != TokenType.EOF) {
                statements.add(expr());
            }
        }

        // 解析完成后检查是否消费完所有 token
        if (currentToken.type() != TokenType.EOF) {
            throw new RuntimeException("位置 " + currentToken.position() + ": 语法错误，表达式后有多余内容 '" + currentToken.value() + "'");
        }

        // 如果只有一个语句，直接返回
        if (statements.size() == 1) {
            return statements.get(0);
        }
        // 多个语句返回 StatementListNode
        return new StatementListNode(statements);
    }
}


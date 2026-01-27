import cn.czyx007.expression_parser.ast.ExprNode;
import cn.czyx007.expression_parser.lexer.Lexer;
import cn.czyx007.expression_parser.parser.Parser;

import java.util.HashMap;
import java.util.Map;

// 主程序测试
public class Test {
    public static void main(String[] args) {
        System.out.println("========== 表达式解析器测试 ==========\n");

        // 1. 浮点数支持
        System.out.println("--- 1. 浮点数运算 ---");
        testExpression("3.14 * 2");      // 预期 6.28
        testExpression("10.5 + 2.5");    // 预期 13.0
        testExpression(".1+.2");         // 预期 0.3

        // 2. 幂运算 (^)
        System.out.println("\n--- 2. 幂运算 (^) ---");
        testExpression("2 ^ 3");         // 预期 8.0
        testExpression("2 ^ 3 ^ 2");     // 预期 512.0 (右结合: 2^(3^2))
        testExpression("2 + 3 ^ 2");     // 预期 11.0 (幂优先: 2 + 9)

        // 3. 取模运算 (%)
        System.out.println("\n--- 3. 取模运算 (%) ---");
        testExpression("10 % 3");        // 预期 1.0
        testExpression("100 % 7 * 2");   // 预期 4.0 (2 * 2)

        // 4. 一元运算符 (负数和正号)
        System.out.println("\n--- 4. 一元运算符 (负号/正号) ---");
        testExpression("-5 + 10");       // 预期 5.0
        testExpression("10 * -2");       // 预期 -20.0
        testExpression("5 * (-3 + 2)");  // 预期 -5.0
        testExpression("--5");           // 预期 5.0 (负负得正)

        // 5. 综合复杂测试
        System.out.println("\n--- 5. 综合复杂测试 ---");
        testExpression("-3.5 ^ 2 + 10 * (2 % 3)");
        // [改进后] 计算: -(3.5^2) + 10*(2) = -12.25 + 20 = 7.75

        // 6. 边界与异常
        System.out.println("\n--- 6. 边界与异常 ---");
        testExpression("10 % 0");        // 预期错误: 模数不能为0
        testExpression("1 2");           // 预期错误: 数字之间不允许隐式乘法
        testExpression(".5 + 1");        // 预期 1.5 (改进点2: 支持省略前导零)
        testExpression("((5))");         // 预期 5.0
        testExpression("");
        testExpression("1 ++ 2");
        testExpression("(1 + 2");

        // ========== 新增功能测试 ==========

        // 7. 科学计数法
        System.out.println("\n--- 7. 科学计数法 ---");
        testExpression("1.5e3");         // 预期 1500.0
        testExpression("1.5E-3");        // 预期 0.0015
        testExpression("2e10 / 1e5");    // 预期 200000.0
        testExpression("1.23e-4 * 1e6"); // 预期 123.0

        // 8. 数学常量
        System.out.println("\n--- 8. 数学常量 (PI, E) ---");
        testExpression("PI");            // 预期 3.141592653589793
        testExpression("E");             // 预期 2.718281828459045
        testExpression("2 * PI");        // 预期 6.283185307179586
        testExpression("PI ^ 2");        // 预期 9.869604401089358

        // 9. 数学函数 - 三角函数
        System.out.println("\n--- 9. 三角函数 ---");
        testExpression("sin(0)");        // 预期 0.0
        testExpression("cos(0)");        // 预期 1.0
        testExpression("tan(0)");        // 预期 0.0
        testExpression("sin(PI/2)");     // 预期 1.0
        testExpression("cos(PI)");       // 预期 -1.0
        testExpression("asin(1)");       // 预期 1.5707963... (PI/2)
        testExpression("acos(0)");       // 预期 1.5707963... (PI/2)
        testExpression("atan(1)");       // 预期 0.7853981... (PI/4)

        // 10. 数学函数 - 对数与指数
        System.out.println("\n--- 10. 对数与指数 ---");
        testExpression("exp(1)");        // 预期 2.718281828459045 (e)
        testExpression("exp(0)");        // 预期 1.0
        testExpression("ln(E)");         // 预期 1.0
        testExpression("ln(1)");         // 预期 0.0
        testExpression("log(100)");      // 预期 2.0 (log10)
        testExpression("log(2, 8)");     // 预期 3.0 (log base 2 of 8)
        testExpression("log(10, 1000)"); // 预期 3.0

        // 11. 数学函数 - 其他实用函数
        System.out.println("\n--- 11. 其他实用函数 ---");
        testExpression("sqrt(16)");      // 预期 4.0
        testExpression("sqrt(2)");       // 预期 1.4142135623730951
        testExpression("abs(-5)");       // 预期 5.0
        testExpression("abs(5)");        // 预期 5.0
        testExpression("ceil(3.2)");     // 预期 4.0
        testExpression("floor(3.8)");    // 预期 3.0
        testExpression("round(3.5)");    // 预期 4.0
        testExpression("round(3.4)");    // 预期 3.0
        testExpression("pow(2, 10)");    // 预期 1024.0
        testExpression("max(3, 7)");     // 预期 7.0
        testExpression("min(3, 7)");     // 预期 3.0

        // 12. 阶乘运算
        System.out.println("\n--- 12. 阶乘运算 (!) ---");
        testExpression("5!");            // 预期 120.0
        testExpression("0!");            // 预期 1.0
        testExpression("1!");            // 预期 1.0
        testExpression("10!");           // 预期 3628800.0
        testExpression("3! + 4!");       // 预期 30.0 (6 + 24)
        testExpression("3! * 2");        // 预期 12.0
        testExpression("(3!)!");         // 预期 720.0 (6!)
        testExpression("(-1)!");         // 预期错误: 阶乘要求非负整数

        // 13. 隐式乘法
        System.out.println("\n--- 13. 隐式乘法 ---");
        testExpression("2PI");           // 预期 6.283185... (2*PI)
        testExpression("3(4+5)");        // 预期 27.0 (3*9)
        testExpression("(2+3)(4+5)");    // 预期 45.0 (5*9)
        testExpression("2 3 4");         // 预期错误: 数字之间不允许隐式乘法
        testExpression("2sqrt(4)");      // 预期 4.0 (2*2)
        testExpression("PIE");           // 预期错误: PIE 作为变量未定义

        // 14. 变量支持（使用分号分隔的多语句表达式）
        System.out.println("\n--- 14. 变量支持 (及变量复用) ---");
        testExpression("price = 100; price * 0.8");   // 预期 80.0
        // 1. 创建一个共用的上下文
        Map<String, Double> sharedContext = new HashMap<>();
        // 2. 将这个 context 依次传入
        testExpression("x = 10; y = 5", sharedContext);
        testExpression("x + y", sharedContext);       // 预期 15.0
        testExpression("x * y", sharedContext);       // 预期 50.0
        testExpression("x ^ 2 + y ^ 2", sharedContext); // 预期 125.0
        testExpression("y = 4; 2x + 3y", sharedContext);     // 预期 32.0 (隐式乘法)
        testExpression("sin(x)", sharedContext);             // 预期 sin(10) ≈ -0.544

        // 15. 赋值操作（完全在表达式中定义变量）
        System.out.println("\n--- 15. 赋值操作 ---");
        testExpression("a = 10 + 5");                 // 预期 15.0
        testExpression("a = 10 + 5; b = a * 2");      // 预期 30.0
        testExpression("a = 10 + 5; b = a * 2; c = a + b; c"); // 预期 45.0
        testExpression("x = 10; y = 2x; y");          // 预期 20.0 (隐式乘法 2*x)
        testExpression("x = 10; y = 2x; x + y");      // 预期 30.0

        // 16. 函数错误处理
        System.out.println("\n--- 16. 函数错误处理 ---");
        testExpression("ln(-1)");        // 预期错误: ln 的参数必须大于 0
        testExpression("sqrt(-4)");      // 预期错误: sqrt 的参数不能为负数
        testExpression("log(0)");        // 预期错误: log 的参数必须大于 0
        testExpression("sin()");         // 预期错误: 函数 sin 需要 1 个参数
        testExpression("unknown(1)");    // 预期错误: 未知的函数

        // 17. 复杂综合表达式
        System.out.println("\n--- 17. 复杂综合表达式 ---");
        testExpression("sin(PI/4)^2 + cos(PI/4)^2");  // 预期 1.0 (三角恒等式)
        testExpression("exp(ln(5))");                  // 预期 5.0
        testExpression("sqrt(3^2 + 4^2)");            // 预期 5.0 (勾股定理)
        testExpression("2^(1+2) * 3!");               // 预期 48.0 (8*6)
        testExpression("log(2, 2^10)");               // 预期 10.0
        testExpression("abs(sin(PI))");               // 表达式值接近 0
        testExpression("floor(PI) + ceil(E)");        // 预期 6.0 (3+3)

        // 圆的面积和周长公式测试
        testExpression("r = 5; PI * r^2");           // 圆面积 ≈ 78.54
        testExpression("r = 5; 2PI r");              // 圆周长 ≈ 31.42 (隐式乘法)
    }

    private static void testExpression(String expression, Map<String, Double> context) {
        System.out.println("表达式: [" + expression + "]");
        try {
            Lexer lexer = new Lexer(expression);
            Parser parser = new Parser(lexer);
            ExprNode root = parser.parse();

            // eval 使用传入的 context
            double result = root.eval(context);
            System.out.println("结果: " + result);
        } catch (Exception e) {
            System.out.println("错误: " + e.getMessage());
        }
        System.out.println("----------------------------------------");
    }

    private static void testExpression(String expression) {
        testExpression(expression, new HashMap<>());
    }
}
import cn.czyx007.expression_parser.ast.ExprNode;
import cn.czyx007.expression_parser.lexer.Lexer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import cn.czyx007.expression_parser.parser.Parser;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Expression Parser 单元测试
 */
class ExpressionParserTest {

    private static final double DELTA = 1e-10;

    private double eval(String expression) {
        return eval(expression, new HashMap<>());
    }

    private double eval(String expression, Map<String, Double> context) {
        Lexer lexer = new Lexer(expression);
        Parser parser = new Parser(lexer);
        ExprNode root = parser.parse();
        return root.eval(context);
    }

    // ==================== 1. 基础运算 ====================
    @Nested
    @DisplayName("基础运算测试")
    class BasicOperations {
        @Test
        @DisplayName("浮点数运算")
        void testFloatOperations() {
            assertEquals(6.28, eval("3.14 * 2"), DELTA);
            assertEquals(13.0, eval("10.5 + 2.5"), DELTA);
            assertEquals(0.3, eval(".1+.2"), DELTA);
            assertEquals(1.5, eval(".5 + 1"), DELTA);
        }

        @Test
        @DisplayName("加减乘除")
        void testBasicArithmetic() {
            assertEquals(7, eval("3 + 4"), DELTA);
            assertEquals(6, eval("10 - 4"), DELTA);
            assertEquals(12, eval("3 * 4"), DELTA);
            assertEquals(2.5, eval("10 / 4"), DELTA);
        }

        @Test
        @DisplayName("括号优先级")
        void testParentheses() {
            assertEquals(5, eval("((5))"), DELTA);
            assertEquals(14, eval("2 * (3 + 4)"), DELTA);
            assertEquals(20, eval("(2 + 3) * 4"), DELTA);
        }
    }

    // ==================== 2. 幂运算 ====================
    @Nested
    @DisplayName("幂运算测试")
    class PowerOperations {
        @Test
        @DisplayName("基础幂运算")
        void testBasicPower() {
            assertEquals(8, eval("2 ^ 3"), DELTA);
            assertEquals(9, eval("3 ^ 2"), DELTA);
        }

        @Test
        @DisplayName("幂运算右结合")
        void testRightAssociative() {
            assertEquals(512, eval("2 ^ 3 ^ 2"), DELTA); // 2^(3^2) = 2^9 = 512
        }

        @Test
        @DisplayName("幂运算优先级")
        void testPowerPrecedence() {
            assertEquals(11, eval("2 + 3 ^ 2"), DELTA); // 2 + 9 = 11
        }
    }

    // ==================== 3. 取模运算 ====================
    @Nested
    @DisplayName("取模运算测试")
    class ModuloOperations {
        @Test
        @DisplayName("基础取模")
        void testBasicModulo() {
            assertEquals(1, eval("10 % 3"), DELTA);
            assertEquals(4, eval("100 % 7 * 2"), DELTA); // (100%7)*2 = 2*2 = 4
        }

        @Test
        @DisplayName("取模除零错误")
        void testModuloByZero() {
            Exception ex = assertThrows(ArithmeticException.class, () -> eval("10 % 0"));
            assertTrue(ex.getMessage().contains("模数不能为0"));
        }
    }

    // ==================== 4. 一元运算符 ====================
    @Nested
    @DisplayName("一元运算符测试")
    class UnaryOperations {
        @Test
        @DisplayName("负号")
        void testNegation() {
            assertEquals(5, eval("-5 + 10"), DELTA);
            assertEquals(-20, eval("10 * -2"), DELTA);
            assertEquals(-5, eval("5 * (-3 + 2)"), DELTA);
        }

        @Test
        @DisplayName("双重负号")
        void testDoubleNegation() {
            assertEquals(5, eval("--5"), DELTA);
        }

        @Test
        @DisplayName("负号与幂运算优先级")
        void testNegationPowerPrecedence() {
            assertEquals(-9, eval("-3^2"), DELTA); // -(3^2) = -9
        }
    }

    // ==================== 5. 科学计数法 ====================
    @Nested
    @DisplayName("科学计数法测试")
    class ScientificNotation {
        @Test
        @DisplayName("科学计数法解析")
        void testScientificNotation() {
            assertEquals(1500, eval("1.5e3"), DELTA);
            assertEquals(0.0015, eval("1.5E-3"), DELTA);
            assertEquals(200000, eval("2e10 / 1e5"), DELTA);
            assertEquals(123, eval("1.23e-4 * 1e6"), DELTA);
        }
    }

    // ==================== 6. 数学常量 ====================
    @Nested
    @DisplayName("数学常量测试")
    class MathConstants {
        @Test
        @DisplayName("PI 和 E")
        void testConstants() {
            assertEquals(Math.PI, eval("PI"), DELTA);
            assertEquals(Math.E, eval("E"), DELTA);
            assertEquals(2 * Math.PI, eval("2 * PI"), DELTA);
            assertEquals(Math.PI * Math.PI, eval("PI ^ 2"), DELTA);
        }
    }

    // ==================== 7. 三角函数 ====================
    @Nested
    @DisplayName("三角函数测试")
    class TrigFunctions {
        @Test
        @DisplayName("sin, cos, tan")
        void testBasicTrig() {
            assertEquals(0, eval("sin(0)"), DELTA);
            assertEquals(1, eval("cos(0)"), DELTA);
            assertEquals(0, eval("tan(0)"), DELTA);
            assertEquals(1, eval("sin(PI/2)"), DELTA);
            assertEquals(-1, eval("cos(PI)"), DELTA);
        }

        @Test
        @DisplayName("反三角函数")
        void testInverseTrig() {
            assertEquals(Math.PI / 2, eval("asin(1)"), DELTA);
            assertEquals(Math.PI / 2, eval("acos(0)"), DELTA);
            assertEquals(Math.PI / 4, eval("atan(1)"), DELTA);
        }

        @Test
        @DisplayName("双曲三角函数")
        void testHyperbolicTrig() {
            assertEquals(Math.sinh(1), eval("sinh(1)"), DELTA);
            assertEquals(Math.cosh(0), eval("cosh(0)"), DELTA);
            assertEquals(Math.tanh(1), eval("tanh(1)"), DELTA);
            assertEquals(0, eval("sinh(0)"), DELTA);
            assertEquals(1, eval("cosh(0)"), DELTA);
        }

        @Test
        @DisplayName("atan2 双参数反正切")
        void testAtan2() {
            assertEquals(Math.PI / 4, eval("atan2(1, 1)"), DELTA);
            assertEquals(Math.PI / 2, eval("atan2(1, 0)"), DELTA);
            assertEquals(0, eval("atan2(0, 1)"), DELTA);
        }
    }

    // ==================== 8. 对数与指数 ====================
    @Nested
    @DisplayName("对数与指数测试")
    class LogExpFunctions {
        @Test
        @DisplayName("exp 函数")
        void testExp() {
            assertEquals(Math.E, eval("exp(1)"), DELTA);
            assertEquals(1, eval("exp(0)"), DELTA);
        }

        @Test
        @DisplayName("ln 函数")
        void testLn() {
            assertEquals(1, eval("ln(E)"), DELTA);
            assertEquals(0, eval("ln(1)"), DELTA);
        }

        @Test
        @DisplayName("log 函数 (单参数和双参数)")
        void testLog() {
            assertEquals(2, eval("log(100)"), DELTA);
            assertEquals(3, eval("log(2, 8)"), DELTA);
            assertEquals(3, eval("log(10, 1000)"), DELTA);
        }

        @Test
        @DisplayName("log10 常用对数")
        void testLog10() {
            assertEquals(2, eval("log10(100)"), DELTA);
            assertEquals(3, eval("log10(1000)"), DELTA);
            assertEquals(0, eval("log10(1)"), DELTA);
        }

        @Test
        @DisplayName("ln 参数错误")
        void testLnError() {
            assertThrows(ArithmeticException.class, () -> eval("ln(-1)"));
        }

        @Test
        @DisplayName("log10 参数错误")
        void testLog10Error() {
            assertThrows(ArithmeticException.class, () -> eval("log10(-1)"));
            assertThrows(ArithmeticException.class, () -> eval("log10(0)"));
        }
    }

    // ==================== 9. 其他数学函数 ====================
    @Nested
    @DisplayName("其他数学函数测试")
    class OtherMathFunctions {
        @Test
        @DisplayName("sqrt")
        void testSqrt() {
            assertEquals(4, eval("sqrt(16)"), DELTA);
            assertEquals(Math.sqrt(2), eval("sqrt(2)"), DELTA);
        }

        @Test
        @DisplayName("cbrt 立方根")
        void testCbrt() {
            assertEquals(3, eval("cbrt(27)"), DELTA);
            assertEquals(2, eval("cbrt(8)"), DELTA);
            assertEquals(-2, eval("cbrt(-8)"), DELTA);
        }

        @Test
        @DisplayName("abs")
        void testAbs() {
            assertEquals(5, eval("abs(-5)"), DELTA);
            assertEquals(5, eval("abs(5)"), DELTA);
        }

        @Test
        @DisplayName("signum 和 sign 符号函数")
        void testSignum() {
            assertEquals(1, eval("signum(5)"), DELTA);
            assertEquals(-1, eval("signum(-5)"), DELTA);
            assertEquals(0, eval("signum(0)"), DELTA);
            assertEquals(1, eval("sign(100)"), DELTA);
            assertEquals(-1, eval("sign(-100)"), DELTA);
        }

        @Test
        @DisplayName("取整函数")
        void testRounding() {
            assertEquals(4, eval("ceil(3.2)"), DELTA);
            assertEquals(3, eval("floor(3.8)"), DELTA);
            assertEquals(4, eval("round(3.5)"), DELTA);
            assertEquals(3, eval("round(3.4)"), DELTA);
        }

        @Test
        @DisplayName("角度弧度转换")
        void testAngleConversion() {
            assertEquals(180, eval("degrees(PI)"), DELTA);
            assertEquals(90, eval("degrees(PI/2)"), DELTA);
            assertEquals(Math.PI, eval("radians(180)"), DELTA);
            assertEquals(Math.PI / 2, eval("radians(90)"), DELTA);
        }

        @Test
        @DisplayName("pow, max, min")
        void testPowMaxMin() {
            assertEquals(1024, eval("pow(2, 10)"), DELTA);
            assertEquals(7, eval("max(3, 7)"), DELTA);
            assertEquals(3, eval("min(3, 7)"), DELTA);
        }

        @Test
        @DisplayName("多参数 max 和 min")
        void testMultiArgMaxMin() {
            assertEquals(9, eval("max(1, 5, 3, 9, 2)"), DELTA);
            assertEquals(1, eval("min(1, 5, 3, 9, 2)"), DELTA);
            assertEquals(100, eval("max(10, 20, 100, 50)"), DELTA);
            assertEquals(10, eval("min(10, 20, 100, 50)"), DELTA);
        }

        @Test
        @DisplayName("统计函数 sum 和 avg")
        void testStatFunctions() {
            assertEquals(15, eval("sum(1, 2, 3, 4, 5)"), DELTA);
            assertEquals(3, eval("avg(1, 2, 3, 4, 5)"), DELTA);
            assertEquals(3, eval("mean(1, 2, 3, 4, 5)"), DELTA);  // mean 是 avg 的别名
            assertEquals(10, eval("sum(10)"), DELTA);
            assertEquals(10, eval("avg(10)"), DELTA);
            assertEquals(100, eval("sum(20, 30, 50)"), DELTA);
            assertEquals(50, eval("avg(30, 40, 80)"), DELTA);
        }

        @Test
        @DisplayName("统计函数 prod, count, median")
        void testMoreStatFunctions() {
            // prod - 乘积
            assertEquals(120, eval("prod(1, 2, 3, 4, 5)"), DELTA);
            assertEquals(24, eval("prod(2, 3, 4)"), DELTA);
            assertEquals(100, eval("prod(10, 10)"), DELTA);
            assertEquals(10, eval("prod(10)"), DELTA);
            assertEquals(120, eval("product(1, 2, 3, 4, 5)"), DELTA);  // product 是 prod 的别名

            // count - 计数
            assertEquals(5, eval("count(1, 2, 3, 4, 5)"), DELTA);
            assertEquals(1, eval("count(100)"), DELTA);
            assertEquals(3, eval("count(10, 20, 30)"), DELTA);

            // gcd / lcm - 整数函数
            assertEquals(4, eval("gcd(8, 12)"), DELTA);
            assertEquals(12, eval("lcm(4, 6)"), DELTA);
            assertEquals(4, eval("gcd(8, 12, 20)"), DELTA);
            assertEquals(60, eval("lcm(4, 6, 10)"), DELTA);

            // range - 极差
            assertEquals(9, eval("range(1, 5, 10)"), DELTA); // 10 - 1 = 9
            assertEquals(0, eval("range(5, 5, 5)"), DELTA);

            // sumabs - 绝对值和 (L1 范数)
            assertEquals(15, eval("sumabs(-1, -2, 3, 4, -5)"), DELTA); // 1+2+3+4+5=15
            assertEquals(20, eval("sumabs(-10, 10)"), DELTA);

            // norm2 - 欧几里得范数 (L2 范数)
            assertEquals(Math.sqrt(14), eval("norm2(1, 2, 3)"), DELTA); // sqrt(1^2+2^2+3^2)=sqrt(14)
            assertEquals(5, eval("norm2(3, 4)"), DELTA);

            // rms - 均方根
            assertEquals(Math.sqrt(14.0 / 3.0), eval("rms(1, 2, 3)"), DELTA); // sqrt((1^2+2^2+3^2)/3)
            assertEquals(Math.sqrt(25.0 / 2.0), eval("rms(3, 4)"), DELTA); // sqrt((3^2+4^2)/2)

            // geomean - 几何平均数
            assertEquals(3, eval("geomean(1, 3, 9)"), DELTA); // (1*3*9)^(1/3)=3
            
            // var / variance (样本方差) and std / stddev (样本标准差)
            // For values 1,2,3 sample variance = 1, standard deviation = 1
            assertEquals(1, eval("var(1, 2, 3)"), DELTA);
            assertEquals(1, eval("variance(1, 2, 3)"), DELTA); // alias
            assertEquals(1, eval("std(1, 2, 3)"), DELTA);
            assertEquals(1, eval("stddev(1, 2, 3)"), DELTA); // alias

            // varp / variancep (总体方差) and stdp / stddevp (总体标准差)
            // For values 1,2,3 population variance = 2/3, standard deviation = sqrt(2/3)
            assertEquals(2.0 / 3.0, eval("varp(1, 2, 3)"), DELTA);
            assertEquals(2.0 / 3.0, eval("variancep(1, 2, 3)"), DELTA); // alias
            assertEquals(Math.sqrt(2.0 / 3.0), eval("stdp(1, 2, 3)"), DELTA);
            assertEquals(Math.sqrt(2.0 / 3.0), eval("stddevp(1, 2, 3)"), DELTA); // alias
            
            // median - 中位数
            assertEquals(3, eval("median(1, 2, 3, 4, 5)"), DELTA);
            assertEquals(3, eval("median(5, 1, 3, 2, 4)"), DELTA);  // 乱序
            assertEquals(3, eval("median(1, 2, 4, 5)"), DELTA);     // 偶数个: (2+4)/2 = 3
            assertEquals(10, eval("median(10)"), DELTA);
            assertEquals(2.5, eval("median(1, 2, 3, 4)"), DELTA);
        }

        @Test
        @DisplayName("hypot 斜边长度")
        void testHypot() {
            assertEquals(5, eval("hypot(3, 4)"), DELTA);
            assertEquals(13, eval("hypot(5, 12)"), DELTA);
            assertEquals(Math.sqrt(2), eval("hypot(1, 1)"), DELTA);
        }

        @Test
        @DisplayName("sqrt 参数错误")
        void testSqrtError() {
            assertThrows(ArithmeticException.class, () -> eval("sqrt(-4)"));
        }
    }

    // ==================== 10. 阶乘 ====================
    @Nested
    @DisplayName("阶乘测试")
    class FactorialOperations {
        @Test
        @DisplayName("基础阶乘")
        void testBasicFactorial() {
            assertEquals(120, eval("5!"), DELTA);
            assertEquals(1, eval("0!"), DELTA);
            assertEquals(1, eval("1!"), DELTA);
            assertEquals(3628800, eval("10!"), DELTA);
        }

        @Test
        @DisplayName("阶乘运算")
        void testFactorialOperations() {
            assertEquals(30, eval("3! + 4!"), DELTA); // 6 + 24
            assertEquals(12, eval("3! * 2"), DELTA);
            assertEquals(720, eval("(3!)!"), DELTA); // 6!
        }

        @Test
        @DisplayName("阶乘错误")
        void testFactorialError() {
            assertThrows(ArithmeticException.class, () -> eval("(-1)!"));
        }
    }

    // ==================== 11. 隐式乘法 ====================
    @Nested
    @DisplayName("隐式乘法测试")
    class ImplicitMultiplication {
        @Test
        @DisplayName("隐式乘法")
        void testImplicitMul() {
            assertEquals(2 * Math.PI, eval("2PI"), DELTA);
            assertEquals(27, eval("3(4+5)"), DELTA); // 3*9
            assertEquals(45, eval("(2+3)(4+5)"), DELTA); // 5*9
            assertEquals(4, eval("2sqrt(4)"), DELTA); // 2*2
        }

        @Test
        @DisplayName("数字间不允许隐式乘法")
        void testNoImplicitMulBetweenNumbers() {
            assertThrows(RuntimeException.class, () -> eval("2 3 4"));
        }
    }

    // ==================== 12. 变量与赋值 ====================
    @Nested
    @DisplayName("变量与赋值测试")
    class VariablesAndAssignment {
        @Test
        @DisplayName("单语句赋值")
        void testSingleAssignment() {
            assertEquals(15, eval("a = 10 + 5"), DELTA);
        }

        @Test
        @DisplayName("多语句赋值")
        void testMultiStatement() {
            assertEquals(80, eval("price = 100; price * 0.8"), DELTA);
            assertEquals(30, eval("a = 10 + 5; b = a * 2"), DELTA);
            assertEquals(45, eval("a = 10 + 5; b = a * 2; c = a + b; c"), DELTA);
        }

        @Test
        @DisplayName("共享上下文")
        void testSharedContext() {
            Map<String, Double> context = new HashMap<>();
            eval("x = 10; y = 5", context);
            assertEquals(15, eval("x + y", context), DELTA);
            assertEquals(50, eval("x * y", context), DELTA);
            assertEquals(125, eval("x ^ 2 + y ^ 2", context), DELTA);
        }

        @Test
        @DisplayName("变量与隐式乘法")
        void testVariableImplicitMul() {
            assertEquals(20, eval("x = 10; y = 2x; y"), DELTA);
            assertEquals(30, eval("x = 10; y = 2x; x + y"), DELTA);
        }

        @Test
        @DisplayName("未定义变量错误")
        void testUndefinedVariable() {
            assertThrows(RuntimeException.class, () -> eval("undefinedVar"));
        }
    }

    // ==================== 13. 综合表达式 ====================
    @Nested
    @DisplayName("综合表达式测试")
    class ComplexExpressions {
        @Test
        @DisplayName("三角恒等式")
        void testTrigIdentity() {
            assertEquals(1, eval("sin(PI/4)^2 + cos(PI/4)^2"), DELTA);
        }

        @Test
        @DisplayName("exp 和 ln 互逆")
        void testExpLnInverse() {
            assertEquals(5, eval("exp(ln(5))"), DELTA);
        }

        @Test
        @DisplayName("勾股定理")
        void testPythagorean() {
            assertEquals(5, eval("sqrt(3^2 + 4^2)"), DELTA);
        }

        @Test
        @DisplayName("复杂组合")
        void testComplexCombinations() {
            assertEquals(48, eval("2^(1+2) * 3!"), DELTA); // 8*6
            assertEquals(10, eval("log(2, 2^10)"), DELTA);
            assertEquals(6, eval("floor(PI) + ceil(E)"), DELTA); // 3+3
        }

        @Test
        @DisplayName("圆的公式")
        void testCircleFormulas() {
            assertEquals(Math.PI * 25, eval("r = 5; PI * r^2"), DELTA); // 圆面积
            assertEquals(2 * Math.PI * 5, eval("r = 5; 2PI r"), DELTA); // 圆周长
        }
    }

    // ==================== 14. 错误处理 ====================
    @Nested
    @DisplayName("错误处理测试")
    class ErrorHandling {
        @Test
        @DisplayName("空表达式")
        void testEmptyExpression() {
            assertThrows(RuntimeException.class, () -> eval(""));
        }

        @Test
        @DisplayName("除零错误")
        void testDivisionByZero() {
            assertThrows(ArithmeticException.class, () -> eval("10 / 0"));
        }

        @Test
        @DisplayName("括号不匹配")
        void testUnmatchedParentheses() {
            assertThrows(RuntimeException.class, () -> eval("(1 + 2"));
        }

        @Test
        @DisplayName("函数参数数量错误")
        void testWrongArgCount() {
            assertThrows(RuntimeException.class, () -> eval("sin()"));
            assertThrows(RuntimeException.class, () -> eval("sin(1, 2)"));
        }

        @Test
        @DisplayName("未知函数")
        void testUnknownFunction() {
            assertThrows(RuntimeException.class, () -> eval("unknown(1)"));
        }
    }
}

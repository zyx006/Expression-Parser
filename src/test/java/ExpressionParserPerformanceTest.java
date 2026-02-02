import cn.czyx007.expression_parser.api.ExpressionEvaluator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Expression Parser 性能压测
 * 使用 System.nanoTime() 计算每次执行的最小、最大、平均耗时
 * <p>
 * 注意：此类标记为 @Disabled，默认不会通过 mvn test 运行。
 * 如需运行性能测试，请在 IDE 中手动运行此类，或使用命令：
 * mvn test -Dtest=ExpressionParserPerformanceTest
 */
@Disabled("性能压测默认禁用，请在 IDE 中手动运行")
class ExpressionParserPerformanceTest {

    // 压测配置
    private static final int BATCH_SIZE = 10;
    private static final int WARMUP_ITERATIONS = 2000;       // 预热次数（增加以确保JIT完成）
    private static final int BENCHMARK_ITERATIONS = 100000;  // 统一压测次数
    private static volatile Object BLACKHOLE;                // 防止DCE的volatile黑洞

    /**
     * 消费结果防止编译器优化（低干扰Blackhole）
     */
    private static void consume(Object result) {
        BLACKHOLE = result;
    }

    /**
     * 压测结果统计类
     */
    static class BenchmarkResult {
        private final String name;
        private final long minNanos;
        private final long maxNanos;
        private final long avgNanos;
        private final long totalNanos;

        public BenchmarkResult(String name, long minNanos, long maxNanos, long avgNanos, long totalNanos) {
            this.name = name;
            this.minNanos = minNanos;
            this.maxNanos = maxNanos;
            this.avgNanos = avgNanos;
            this.totalNanos = totalNanos;
        }

        @Override
        public String toString() {
            return String.format(
                "[%s] 最小: %,d ns (%.3f μs), 最大: %,d ns (%.3f μs), 平均: %,d ns (%.3f μs), 总计: %.3f ms",
                name,
                minNanos, minNanos / 1000.0,
                maxNanos, maxNanos / 1000.0,
                avgNanos, avgNanos / 1000.0,
                totalNanos / 1_000_000.0
            );
        }
    }

    /**
     * 执行压测 - 使用批量测量减少nanoTime开销，并防止DCE优化
     * @param name 压测名称
     * @param expression 表达式
     * @param context 上下文
     * @return 压测结果
     */
    private BenchmarkResult benchmark(String name, String expression, Map<String, Object> context) {
        // 预热 - 使用blackhole确保JIT不会优化掉
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            consume(ExpressionEvaluator.eval(expression, context));
        }

        // 正式压测 - 批量测量减少nanoTime开销
        // 每BATCH_SIZE次调用测量一次，降低nanoTime本身的开销占比
        int batchCount = BENCHMARK_ITERATIONS / BATCH_SIZE;
        long[] batchNanos = new long[batchCount];

        for (int i = 0; i < batchCount; i++) {
            long start = System.nanoTime();
            for (int j = 0; j < BATCH_SIZE; j++) {
                consume(ExpressionEvaluator.eval(expression, context));
            }
            long end = System.nanoTime();
            batchNanos[i] = (end - start) / BATCH_SIZE; // 单次平均耗时
        }

        // 只过滤无效值（<=0），保留所有真实测量结果
        // 不过滤outliers - JVM的抖动是真实存在的，不应该被抹掉
        long minNanos = Long.MAX_VALUE;
        long maxNanos = Long.MIN_VALUE;
        long totalNanos = 0;
        int validCount = 0;

        for (long duration : batchNanos) {
            if (duration > 0) {  // 只过滤时钟回拨等无效值
                minNanos = Math.min(minNanos, duration);
                maxNanos = Math.max(maxNanos, duration);
                totalNanos += duration;
                validCount++;
            }
        }

        // 保底值
        if (minNanos == Long.MAX_VALUE) minNanos = 1;
        if (maxNanos == Long.MIN_VALUE) maxNanos = 1;
        if (validCount == 0) validCount = 1;

        long avgNanos = totalNanos / validCount;
        return new BenchmarkResult(name, minNanos, maxNanos, avgNanos, totalNanos * BATCH_SIZE);
    }

    private BenchmarkResult benchmark(String name, String expression) {
        return benchmark(name, expression, new HashMap<>());
    }

    // ==================== 1. 基础运算压测 ====================
    @Nested
    @DisplayName("基础运算性能压测")
    class BasicOperationsBenchmark {

        @Test
        @DisplayName("简单加减乘除")
        void testBasicArithmetic() {
            System.out.println("\n========== 基础运算压测 ==========");
            System.out.println(benchmark("加法: 3 + 4", "3 + 4"));
            System.out.println(benchmark("减法: 10 - 4", "10 - 4"));
            System.out.println(benchmark("乘法: 3 * 4", "3 * 4"));
            System.out.println(benchmark("除法: 10 / 4", "10 / 4"));
        }

        @Test
        @DisplayName("浮点数运算")
        void testFloatOperations() {
            System.out.println("\n========== 浮点数运算压测 ==========");
            System.out.println(benchmark("浮点乘法: 3.14 * 2", "3.14 * 2"));
            System.out.println(benchmark("浮点加法: 10.5 + 2.5", "10.5 + 2.5"));
            System.out.println(benchmark("小数运算: .1 + .2", ".1 + .2"));
        }

        @Test
        @DisplayName("括号优先级")
        void testParentheses() {
            System.out.println("\n========== 括号优先级压测 ==========");
            System.out.println(benchmark("单层括号: (2 + 3) * 4", "(2 + 3) * 4"));
            System.out.println(benchmark("多层括号: ((2 + 3) * 4 - 5) / 3", "((2 + 3) * 4 - 5) / 3"));
            System.out.println(benchmark("嵌套括号: (((5)))", "(((5)))"));
        }
    }

    // ==================== 2. 幂运算与取模压测 ====================
    @Nested
    @DisplayName("幂运算与取模性能压测")
    class PowerAndModuloBenchmark {

        @Test
        @DisplayName("幂运算")
        void testPowerOperations() {
            System.out.println("\n========== 幂运算压测 ==========");
            System.out.println(benchmark("简单幂: 2 ^ 3", "2 ^ 3"));
            System.out.println(benchmark("右结合幂: 2 ^ 3 ^ 2", "2 ^ 3 ^ 2"));
            System.out.println(benchmark("幂与加法: 2 + 3 ^ 2", "2 + 3 ^ 2"));
            System.out.println(benchmark("大数幂: 10 ^ 6", "10 ^ 6"));
        }

        @Test
        @DisplayName("取模运算")
        void testModuloOperations() {
            System.out.println("\n========== 取模运算压测 ==========");
            System.out.println(benchmark("简单取模: 10 % 3", "10 % 3"));
            System.out.println(benchmark("取模与乘法: 100 % 7 * 2", "100 % 7 * 2"));
        }
    }

    // ==================== 3. 科学计数法压测 ====================
    @Nested
    @DisplayName("科学计数法性能压测")
    class ScientificNotationBenchmark {

        @Test
        @DisplayName("科学计数法解析")
        void testScientificNotation() {
            System.out.println("\n========== 科学计数法压测 ==========");
            System.out.println(benchmark("正指数: 1.5e3", "1.5e3"));
            System.out.println(benchmark("负指数: 1.5E-3", "1.5E-3"));
            System.out.println(benchmark("科学计数法运算: 2e10 / 1e5", "2e10 / 1e5"));
        }
    }

    // ==================== 4. 数学常量压测 ====================
    @Nested
    @DisplayName("数学常量性能压测")
    class MathConstantsBenchmark {

        @Test
        @DisplayName("PI 和 E")
        void testConstants() {
            System.out.println("\n========== 数学常量压测 ==========");
            System.out.println(benchmark("PI常量", "PI"));
            System.out.println(benchmark("E常量", "E"));
            System.out.println(benchmark("PI运算: 2 * PI", "2 * PI"));
            System.out.println(benchmark("PI幂运算: PI ^ 2", "PI ^ 2"));
        }
    }

    // ==================== 5. 三角函数压测 ====================
    @Nested
    @DisplayName("三角函数性能压测")
    class TrigFunctionsBenchmark {

        @Test
        @DisplayName("基础三角函数")
        void testBasicTrig() {
            System.out.println("\n========== 基础三角函数压测 ==========");
            System.out.println(benchmark("sin(0)", "sin(0)"));
            System.out.println(benchmark("cos(0)", "cos(0)"));
            System.out.println(benchmark("tan(0)", "tan(0)"));
            System.out.println(benchmark("sin(PI/2)", "sin(PI/2)"));
            System.out.println(benchmark("cos(PI)", "cos(PI)"));
        }

        @Test
        @DisplayName("反三角函数")
        void testInverseTrig() {
            System.out.println("\n========== 反三角函数压测 ==========");
            System.out.println(benchmark("asin(1)", "asin(1)"));
            System.out.println(benchmark("acos(0)", "acos(0)"));
            System.out.println(benchmark("atan(1)", "atan(1)"));
            System.out.println(benchmark("atan2(1, 1)", "atan2(1, 1)"));
        }

        @Test
        @DisplayName("双曲三角函数")
        void testHyperbolicTrig() {
            System.out.println("\n========== 双曲三角函数压测 ==========");
            System.out.println(benchmark("sinh(1)", "sinh(1)"));
            System.out.println(benchmark("cosh(0)", "cosh(0)"));
            System.out.println(benchmark("tanh(1)", "tanh(1)"));
        }
    }

    // ==================== 6. 对数与指数压测 ====================
    @Nested
    @DisplayName("对数与指数性能压测")
    class LogExpFunctionsBenchmark {

        @Test
        @DisplayName("指数和对数")
        void testExpLog() {
            System.out.println("\n========== 对数与指数压测 ==========");
            System.out.println(benchmark("exp(1)", "exp(1)"));
            System.out.println(benchmark("ln(E)", "ln(E)"));
            System.out.println(benchmark("log(100)", "log(100)"));
            System.out.println(benchmark("log(2, 8)", "log(2, 8)"));
            System.out.println(benchmark("log10(100)", "log10(100)"));
        }
    }

    // ==================== 7. 其他数学函数压测 ====================
    @Nested
    @DisplayName("其他数学函数性能压测")
    class OtherMathFunctionsBenchmark {

        @Test
        @DisplayName("根号与绝对值")
        void testSqrtAbs() {
            System.out.println("\n========== 根号与绝对值压测 ==========");
            System.out.println(benchmark("sqrt(16)", "sqrt(16)"));
            System.out.println(benchmark("cbrt(27)", "cbrt(27)"));
            System.out.println(benchmark("abs(-5)", "abs(-5)"));
            System.out.println(benchmark("hypot(3, 4)", "hypot(3, 4)"));
        }

        @Test
        @DisplayName("取整函数")
        void testRounding() {
            System.out.println("\n========== 取整函数压测 ==========");
            System.out.println(benchmark("ceil(3.2)", "ceil(3.2)"));
            System.out.println(benchmark("floor(3.8)", "floor(3.8)"));
            System.out.println(benchmark("round(3.5)", "round(3.5)"));
        }

        @Test
        @DisplayName("极值与幂函数")
        void testMinMaxPow() {
            System.out.println("\n========== 极值与幂函数压测 ==========");
            System.out.println(benchmark("pow(2, 10)", "pow(2, 10)"));
            System.out.println(benchmark("max(3, 7)", "max(3, 7)"));
            System.out.println(benchmark("min(3, 7)", "min(3, 7)"));
            System.out.println(benchmark("多参数max: max(1, 5, 3, 9, 2)", "max(1, 5, 3, 9, 2)"));
        }

        @Test
        @DisplayName("角度转换")
        void testAngleConversion() {
            System.out.println("\n========== 角度转换压测 ==========");
            System.out.println(benchmark("degrees(PI)", "degrees(PI)"));
            System.out.println(benchmark("radians(180)", "radians(180)"));
        }
    }

    // ==================== 8. 统计函数压测 ====================
    @Nested
    @DisplayName("统计函数性能压测")
    class StatisticalFunctionsBenchmark {

        @Test
        @DisplayName("基础统计函数")
        void testBasicStats() {
            System.out.println("\n========== 基础统计函数压测 ==========");
            System.out.println(benchmark("sum(1, 2, 3, 4, 5)", "sum(1, 2, 3, 4, 5)"));
            System.out.println(benchmark("avg(1, 2, 3, 4, 5)", "avg(1, 2, 3, 4, 5)"));
            System.out.println(benchmark("prod(1, 2, 3, 4, 5)", "prod(1, 2, 3, 4, 5)"));
            System.out.println(benchmark("count(1, 2, 3, 4, 5)", "count(1, 2, 3, 4, 5)"));
        }

        @Test
        @DisplayName("高级统计函数")
        void testAdvancedStats() {
            System.out.println("\n========== 高级统计函数压测 ==========");
            System.out.println(benchmark("median(1, 2, 3, 4, 5)", "median(1, 2, 3, 4, 5)"));
            System.out.println(benchmark("std(1, 2, 3)", "std(1, 2, 3)"));
            System.out.println(benchmark("var(1, 2, 3)", "var(1, 2, 3)"));
            System.out.println(benchmark("geomean(1, 3, 9)", "geomean(1, 3, 9)"));
        }

        @Test
        @DisplayName("百分位数与范数")
        void testPercentileAndNorm() {
            System.out.println("\n========== 百分位数与范数压测 ==========");
            System.out.println(benchmark("percentile(50, 1, 2, 3, 4, 5)", "percentile(50, 1, 2, 3, 4, 5)"));
            System.out.println(benchmark("norm2(1, 2, 3)", "norm2(1, 2, 3)"));
            System.out.println(benchmark("sumabs(-1, -2, 3, 4, -5)", "sumabs(-1, -2, 3, 4, -5)"));
        }
    }

    // ==================== 9. 阶乘压测 ====================
    @Nested
    @DisplayName("阶乘性能压测")
    class FactorialBenchmark {

        @Test
        @DisplayName("阶乘运算")
        void testFactorial() {
            System.out.println("\n========== 阶乘压测 ==========");
            System.out.println(benchmark("5!", "5!"));
            System.out.println(benchmark("10!", "10!"));
            System.out.println(benchmark("阶乘运算: 3! + 4!", "3! + 4!"));
            System.out.println(benchmark("嵌套阶乘: (3!)!", "(3!)!"));
        }
    }

    // ==================== 10. 隐式乘法压测 ====================
    @Nested
    @DisplayName("隐式乘法性能压测")
    class ImplicitMultiplicationBenchmark {

        @Test
        @DisplayName("隐式乘法")
        void testImplicitMul() {
            System.out.println("\n========== 隐式乘法压测 ==========");
            System.out.println(benchmark("2PI", "2PI"));
            System.out.println(benchmark("3(4+5)", "3(4+5)"));
            System.out.println(benchmark("(2+3)(4+5)", "(2+3)(4+5)"));
            System.out.println(benchmark("2sqrt(4)", "2sqrt(4)"));
        }
    }

    // ==================== 11. 变量与赋值压测 ====================
    @Nested
    @DisplayName("变量与赋值性能压测")
    class VariablesBenchmark {

        @Test
        @DisplayName("单语句赋值")
        void testSingleAssignment() {
            System.out.println("\n========== 单语句赋值压测 ==========");
            System.out.println(benchmark("a = 10 + 5", "a = 10 + 5"));
        }

        @Test
        @DisplayName("多语句赋值")
        void testMultiStatement() {
            System.out.println("\n========== 多语句赋值压测 ==========");
            System.out.println(benchmark("price = 100; price * 0.8", "price = 100; price * 0.8"));
            System.out.println(benchmark("a = 10 + 5; b = a * 2", "a = 10 + 5; b = a * 2"));
            System.out.println(benchmark("三语句: a=10+5; b=a*2; c=a+b; c", "a = 10 + 5; b = a * 2; c = a + b; c"));
        }

        @Test
        @DisplayName("带上下文的变量")
        void testVariableWithContext() {
            System.out.println("\n========== 带上下文变量压测 ==========");
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("x", 10.0);
            ctx.put("y", 5.0);
            System.out.println(benchmark("上下文变量: x + y", "x + y", ctx));
            System.out.println(benchmark("上下文变量: x * y", "x * y", ctx));
        }
    }

    // ==================== 12. 综合表达式压测 ====================
    @Nested
    @DisplayName("综合表达式性能压测")
    class ComplexExpressionsBenchmark {

        @Test
        @DisplayName("三角恒等式")
        void testTrigIdentity() {
            System.out.println("\n========== 三角恒等式压测 ==========");
            System.out.println(benchmark("sin(PI/4)^2 + cos(PI/4)^2", "sin(PI/4)^2 + cos(PI/4)^2"));
        }

        @Test
        @DisplayName("复杂组合表达式")
        void testComplexCombinations() {
            System.out.println("\n========== 复杂组合表达式压测 ==========");
            System.out.println(benchmark("2^(1+2) * 3!", "2^(1+2) * 3!"));
            System.out.println(benchmark("log(2, 2^10)", "log(2, 2^10)"));
            System.out.println(benchmark("floor(PI) + ceil(E)", "floor(PI) + ceil(E)"));
        }

        @Test
        @DisplayName("圆的公式")
        void testCircleFormulas() {
            System.out.println("\n========== 圆的公式压测 ==========");
            System.out.println(benchmark("圆面积: r=5; PI*r^2", "r = 5; PI * r^2"));
            System.out.println(benchmark("圆周长: r=5; 2PI r", "r = 5; 2PI r"));
        }

        @Test
        @DisplayName("复杂嵌套表达式")
        void testDeepNesting() {
            System.out.println("\n========== 复杂嵌套表达式压测 ==========");
            System.out.println(benchmark("多层嵌套: sqrt(3^2 + 4^2) + sin(PI/2)", "sqrt(3^2 + 4^2) + sin(PI/2)"));
            System.out.println(benchmark("统计嵌套: max(sin(0), cos(0), tan(PI/4))", "max(sin(0), cos(0), tan(PI/4))"));
            System.out.println(benchmark("混合运算: log(10, 1000) + exp(ln(5)) - abs(-10)", "log(10, 1000) + exp(ln(5)) - abs(-10)"));
        }
    }

    // ==================== 13. 数组操作压测 ====================
    @Nested
    @DisplayName("数组操作性能压测")
    class ArrayOperationsBenchmark {

        @Test
        @DisplayName("数组字面量")
        void testArrayLiteral() {
            System.out.println("\n========== 数组字面量压测 ==========");
            System.out.println(benchmark("[1, 2, 3]", "[1, 2, 3]"));
            System.out.println(benchmark("[1.5, 2.5, 3.5, 4.5, 5.5]", "[1.5, 2.5, 3.5, 4.5, 5.5]"));
        }

        @Test
        @DisplayName("数组统计函数")
        void testArrayStats() {
            System.out.println("\n========== 数组统计函数压测 ==========");
            Map<String, Object> ctx = new HashMap<>();
            ExpressionEvaluator.eval("data = [1,2,3,4,5,6,7,8,9,10]", ctx);
            System.out.println(benchmark("数组sum: sum(data)", "sum(data)", ctx));
            System.out.println(benchmark("数组avg: avg(data)", "avg(data)", ctx));
            System.out.println(benchmark("数组max: max(data)", "max(data)", ctx));
            System.out.println(benchmark("数组median: median(data)", "median(data)", ctx));
        }

        @Test
        @DisplayName("二维数组（矩阵）")
        void testMatrix() {
            System.out.println("\n========== 矩阵操作压测 ==========");
            System.out.println(benchmark("2x2矩阵: [[1,2],[3,4]]", "[[1,2],[3,4]]"));
            System.out.println(benchmark("3x3矩阵: [[1,2,3],[4,5,6],[7,8,9]]", "[[1,2,3],[4,5,6],[7,8,9]]"));
        }

        @Test
        @DisplayName("矩阵运算")
        void testMatrixOperations() {
            System.out.println("\n========== 矩阵运算压测 ==========");
            System.out.println(benchmark("矩阵转置: transpose([[1,2],[3,4]])", "transpose([[1,2],[3,4]])"));
            System.out.println(benchmark("矩阵行列式: det([[1,2],[3,4]])", "det([[1,2],[3,4]])"));
            System.out.println(benchmark("矩阵乘法: matmul([[1,2],[3,4]], [[5],[6]])", "matmul([[1,2],[3,4]], [[5],[6]])"));
        }

        @Test
        @DisplayName("矩阵求逆")
        void testMatrixInverse() {
            System.out.println("\n========== 矩阵求逆压测 ==========");
            System.out.println(benchmark("2x2矩阵求逆: inv([[1,2],[3,4]])", "inv([[1,2],[3,4]])"));
            System.out.println(benchmark("2x2单位矩阵求逆: inv([[1,0],[0,1]])", "inv([[1,0],[0,1]])"));
            System.out.println(benchmark("3x3矩阵求逆: inv([[1,2,3],[0,1,4],[5,6,0]])", "inv([[1,2,3],[0,1,4],[5,6,0]])"));
            System.out.println(benchmark("4x4矩阵求逆", "inv([[4,7,2,3],[2,5,6,4],[8,3,1,9],[6,2,8,5]])"));
            System.out.println(benchmark("验证求逆: matmul([[1,2],[3,4]], inv([[1,2],[3,4]]))",
                "matmul([[1,2],[3,4]], inv([[1,2],[3,4]]))"));
        }

        @Test
        @DisplayName("解线性方程组")
        void testSolveLinear() {
            System.out.println("\n========== 解线性方程组压测 ==========");
            System.out.println(benchmark("2x2方程组: solve([[2,1],[1,-1]], [[5],[1]])",
                "solve([[2,1],[1,-1]], [[5],[1]])"));
            System.out.println(benchmark("3x3对角方程组: solve([[2,0,0],[0,3,0],[0,0,5]], [[4],[9],[15]])",
                "solve([[2,0,0],[0,3,0],[0,0,5]], [[4],[9],[15]])"));
            System.out.println(benchmark("3x3一般方程组: solve([[1,2,3],[0,1,4],[5,6,0]], [[4],[9],[15]])",
                "solve([[1,2,3],[0,1,4],[5,6,0]], [[4],[9],[15]])"));
            System.out.println(benchmark("4x4方程组",
                "solve([[4,7,2,3],[2,5,6,4],[8,3,1,9],[6,2,8,5]], [[10],[20],[30],[40]])"));
            System.out.println(benchmark("验证2x2解: matmul([[2,1],[1,-1]], solve([[2,1],[1,-1]], [[5],[1]]))",
                "matmul([[2,1],[1,-1]], solve([[2,1],[1,-1]], [[5],[1]]))"));
        }

        @Test
        @DisplayName("矩阵其他运算")
        void testOtherMatrixOps() {
            System.out.println("\n========== 矩阵其他运算压测 ==========");
            System.out.println(benchmark("矩阵迹: trace([[1,2],[3,4]])", "trace([[1,2],[3,4]])"));
            System.out.println(benchmark("矩阵秩: rank([[1,2],[3,4]])", "rank([[1,2],[3,4]])"));
            System.out.println(benchmark("矩阵均值(axis=0): mean([[1,2],[3,4]], 0)", "mean([[1,2],[3,4]], 0)"));
            System.out.println(benchmark("矩阵均值(axis=1): mean([[1,2],[3,4]], 1)", "mean([[1,2],[3,4]], 1)"));
        }
    }

    // ==================== 14. 组合数学压测 ====================
    @Nested
    @DisplayName("组合数学性能压测")
    class CombinatoricsBenchmark {

        @Test
        @DisplayName("组合数与排列数")
        void testCombinationPermutation() {
            System.out.println("\n========== 组合数学压测 ==========");
            System.out.println(benchmark("组合数 C(5,2)", "C(5,2)"));
            System.out.println(benchmark("组合数 C(10,5)", "C(10,5)"));
            System.out.println(benchmark("排列数 P(5,2)", "P(5,2)"));
            System.out.println(benchmark("排列数 P(10,5)", "P(10,5)"));
        }

        @Test
        @DisplayName("GCD和LCM")
        void testGcdLcm() {
            System.out.println("\n========== GCD/LCM压测 ==========");
            System.out.println(benchmark("gcd(8, 12)", "gcd(8, 12)"));
            System.out.println(benchmark("lcm(4, 6)", "lcm(4, 6)"));
            System.out.println(benchmark("多参数gcd: gcd(8, 12, 20)", "gcd(8, 12, 20)"));
        }
    }

    // ==================== 15. 协方差与相关性压测 ====================
    @Nested
    @DisplayName("协方差与相关性性能压测")
    class CovarianceCorrelationBenchmark {

        @Test
        @DisplayName("协方差与相关系数")
        void testCovCorr() {
            System.out.println("\n========== 协方差与相关性压测 ==========");
            System.out.println(benchmark("cov([1,2,3], [1,2,3])", "cov([1,2,3], [1,2,3])"));
            System.out.println(benchmark("covp(1,2,3, 1,2,3)", "covp(1,2,3, 1,2,3)"));
            System.out.println(benchmark("corr(1,2,3, 1,2,3)", "corr(1,2,3, 1,2,3)"));
        }

        @Test
        @DisplayName("向量运算")
        void testVectorOps() {
            System.out.println("\n========== 向量运算压测 ==========");
            System.out.println(benchmark("点积: dot([1,2,3], [4,5,6])", "dot([1,2,3], [4,5,6])"));
            System.out.println(benchmark("距离: dist(0,0, 3,4)", "dist(0,0, 3,4)"));
            System.out.println(benchmark("曼哈顿距离: manhattan(0,0, 3,4)", "manhattan(0,0, 3,4)"));
        }
    }

    // ==================== 16. 大批量压测 ====================
    @Nested
    @DisplayName("大批量性能压测")
    class LargeScaleBenchmark {

        @Test
        @DisplayName("大批量基础运算")
        void testLargeScaleBasic() {
            System.out.println("\n========== 大批量压测 (10万次) ==========");
            System.out.println(benchmark("10万次: 3 + 4", "3 + 4"));
            System.out.println(benchmark("10万次: 2 * 3", "2 * 3"));
            System.out.println(benchmark("10万次: sin(0)", "sin(0)"));
        }

        @Test
        @DisplayName("复杂表达式大批量")
        void testLargeScaleComplex() {
            System.out.println("\n========== 复杂表达式大批量压测 (10万次) ==========");
            System.out.println(benchmark("10万次: sqrt(3^2 + 4^2)", "sqrt(3^2 + 4^2)"));
            System.out.println(benchmark("10万次: log(2, 2^10)", "log(2, 2^10)"));
            System.out.println(benchmark("10万次: max(1,2,3,4,5)", "max(1,2,3,4,5)"));
        }
    }
}

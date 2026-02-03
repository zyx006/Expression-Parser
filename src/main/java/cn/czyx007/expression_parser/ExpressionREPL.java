package cn.czyx007.expression_parser;

import cn.czyx007.expression_parser.api.ExpressionEvaluator;
import cn.czyx007.expression_parser.ast.Value;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * REPL (Read-Eval-Print Loop) 交互式计算器
 * 支持变量持久化、历史结果引用等功能
 */
public class ExpressionREPL {
    // 语言枚举
    private enum Language {
        ENGLISH, CHINESE
    }

    // 当前语言（默认英文）
    private static Language currentLanguage = Language.ENGLISH;

    // 共享变量上下文（变量在会话中持久化）- 支持标量和数组
    private static final Map<String, Object> context = new HashMap<>();

    static {
        context.put("ans", new Value(0.0));
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8.name()));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8.name()));

        Scanner scanner = new Scanner(System.in);

        printWelcome();

        while (true) {
            System.out.print(">>> ");
            String input = scanner.nextLine().trim();

            // 空输入跳过
            if (input.isEmpty()) {
                continue;
            }

            // 退出命令
            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("q")) {
                System.out.println(getMessage("goodbye"));
                break;
            }

            // 帮助命令
            if (input.equalsIgnoreCase("help") || input.equals("?")) {
                printHelp();
                continue;
            }

            // 语言切换命令
            if (input.equalsIgnoreCase("lang")) {
                toggleLanguage();
                continue;
            }

            // 清除变量命令
            if (input.equalsIgnoreCase("clear")) {
                Object ansValue = context.getOrDefault("ans", new Value(0.0));
                context.clear();
                context.put("ans", ansValue);
                System.out.println(getMessage("cleared"));
                continue;
            }

            // 显示变量命令
            if (input.equalsIgnoreCase("vars")) {
                printVariables();
                continue;
            }

            // 计算表达式
            try {
                Value result = ExpressionEvaluator.eval(input, context);
                context.put("ans", result);
                System.out.println(result.toString());
            } catch (Exception e) {
                System.out.println(getMessage("error") + e.getMessage());
            }
        }

        scanner.close();
    }

    // 切换语言
    private static void toggleLanguage() {
        currentLanguage = (currentLanguage == Language.ENGLISH) ? Language.CHINESE : Language.ENGLISH;
        printWelcome();
        System.out.println(getMessage("langSwitched"));
    }

    // 获取多语言消息
    private static String getMessage(String key) {
        if (currentLanguage == Language.ENGLISH) {
            switch (key) {
                case "goodbye": return "Goodbye!";
                case "cleared": return "All variables cleared.";
                case "error": return "Error: ";
                case "noVars": return "No variables defined.";
                case "definedVars": return "Defined variables:";
                case "langSwitched": return "Language switched to English.";
                default: return "";
            }
        } else {
            switch (key) {
                case "goodbye": return "再见！";
                case "cleared": return "已清除所有变量。";
                case "error": return "错误: ";
                case "noVars": return "当前没有定义任何变量。";
                case "definedVars": return "已定义的变量:";
                case "langSwitched": return "语言已切换为中文。";
                default: return "";
            }
        }
    }

    private static void printWelcome() {
        if (currentLanguage == Language.ENGLISH) {
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║    Welcome to Expression Parser Interactive Calculator   ║");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║ Enter math expressions to calculate, type 'help' for help║");
            System.out.println("║   Type 'lang' to switch language | Type 'exit' to quit   ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
        } else {
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║       欢迎使用 Expression Parser 交互式计算器            ║");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  输入数学表达式进行计算，输入 'help' 查看帮助            ║");
            System.out.println("║  输入 'lang' 切换语言 | 输入 'exit' 退出                 ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
        }
        System.out.println();
    }

    private static void printHelp() {
        if (currentLanguage == Language.ENGLISH) {
            printHelpEnglish();
        } else {
            printHelpChinese();
        }
    }

    private static void printHelpEnglish() {
        System.out.println();
        System.out.println("=== Help ===");
        System.out.println("【Basic Operations】");
        System.out.println("  +, -, *, /     Addition, subtraction, multiplication, division");
        System.out.println("  %              Modulo");
        System.out.println("  ^              Power (right-associative, e.g., 2^3^2 = 512)");
        System.out.println("  !              Factorial (e.g., 5! = 120)");
        System.out.println("【Constants】");
        System.out.println("  PI             Pi constant π ≈ 3.14159...");
        System.out.println("  E              Euler's number e ≈ 2.71828...");
        System.out.println("【Functions】");
        System.out.println("  sin, cos, tan          Trigonometric functions");
        System.out.println("  asin, acos, atan       Inverse trigonometric functions");
        System.out.println("  sinh, cosh, tanh       Hyperbolic functions");
        System.out.println("  exp, ln, log, log10    Exponential and logarithmic");
        System.out.println("  sqrt, cbrt, abs        Square root, cube root, absolute value");
        System.out.println("  ceil, floor, round     Rounding functions");
        System.out.println("  signum, sign           Sign function (-1, 0, 1)");
        System.out.println("  degrees, radians       Angle conversion");
        System.out.println("  atan2(y,x), hypot(x,y) Two-argument arctangent / hypotenuse");
        System.out.println("  pow(base, exponent)    Power");
        System.out.println();

        System.out.println("  For vectors X = [...]; Y = [...]; (or manually expand):");
        System.out.println("  max(X), min(X)      Multi-argument max/min (2+ args)");
        System.out.println("  sum(X), count(X)    Sum (1+ args), count arguments");
        System.out.println("  avg(X), median(X)   Average / median (1+ args)");
        System.out.println("  prod(X), product(X) Product (1+ args)");
        System.out.println();

        System.out.println("  gcd(X), lcm(X)        Greatest common divisor / least common multiple (integers, 2+ args)");
        System.out.println("  range(X), geomean(X)  Range / geometric mean (1+ positive args)");
        System.out.println("  norm1(X), sumabs(X)   Sum of absolute values (L1 norm)");
        System.out.println("  norm2(X), rms(X)      Euclidean norm (L2) / root mean square");
        System.out.println("  var(X), variance(X)   Sample variance (≥2)");
        System.out.println("  std(X), stddev(X)     Sample standard deviation (≥2)");
        System.out.println("  varp(X), variancep(X) Population variance (≥1)");
        System.out.println("  stdp(X), stddevp(X)   Population standard deviation (≥1)");
        System.out.println();

        System.out.println("  percentile(p, X), pctl(p, X)                Percentile (p ∈ [0,100])");
        System.out.println("  cov(X, Y), covariance(X, Y)                 Sample covariance");
        System.out.println("  covp(X, Y), covariancep(X, Y)               Population covariance");
        System.out.println("  corr(X, Y), correlation(X, Y)               Correlation coefficient");
        System.out.println("  dot(X, Y), dotprod(X, Y)                    Vector dot product");
        System.out.println("  dist(X, Y), distance(X, Y), euclidean(X, Y) Euclidean distance");
        System.out.println("  manhattan(X, Y), taxicab(X, Y)              Manhattan distance");
        System.out.println();

        System.out.println("  transpose(matrix), t(matrix)     Matrix transpose");
        System.out.println("  det(matrix), determinant(matrix) Determinant");
        System.out.println("  matmul(matrix_A, matrix_B)       Matrix multiplication");
        System.out.println("  trace(matrix)                    Matrix trace (sum of diagonal)");
        System.out.println("  rank(matrix)                     Matrix rank");
        System.out.println("  mean(matrix, axis)               Matrix mean (axis=0 cols, axis=1 rows)");
        System.out.println("  inv(matrix)                      Matrix inverse");
        System.out.println("  solve(A, b)                      Solve linear system Ax=b");
        System.out.println();

        System.out.println("【Combinatorics】");
        System.out.println("  C(n,k), comb(n,k)    Combinations");
        System.out.println("  P(n,k), perm(n,k)    Permutations");
        System.out.println();

        System.out.println("【Variables】");
        System.out.println("  x = 10                 Assignment");
        System.out.println("  x = 10; y = 2x         Multiple statements (semicolon-separated)");
        System.out.println("  ans                    Last result");

        System.out.println("【Arrays】");
        System.out.println("  [1, 2, 3]              Array literal");
        System.out.println("  [[1,2], [3,4]]         2D array (matrix)");
        System.out.println("  scores = [1, 2, 3]     Array variable assignment");
        System.out.println("  avg(scores)            Array as function parameter");

        System.out.println("【Implicit Multiplication】");
        System.out.println("  2PI, 3(4+5), 2sqrt(4)  Automatically recognize multiplication");

        System.out.println("【Commands】");
        System.out.println("  help, ?        Show help");
        System.out.println("  vars           Show defined variables");
        System.out.println("  clear          Clear all variables");
        System.out.println("  lang           Toggle language (English ⇄ Chinese)");
        System.out.println("  exit, quit, q  Exit");
        System.out.println();
    }

    private static void printHelpChinese() {
        System.out.println();
        System.out.println("=== 帮助信息 ===");
        System.out.println("【基本运算】");
        System.out.println("  +, -, *, /     加减乘除");
        System.out.println("  %              取模");
        System.out.println("  ^              幂运算 (右结合，如 2^3^2 = 512)");
        System.out.println("  !              阶乘 (如 5! = 120)");
        System.out.println("【常量】");
        System.out.println("  PI             圆周率 π ≈ 3.14159...");
        System.out.println("  E              自然常数 e ≈ 2.71828...");
        System.out.println("【函数】");
        System.out.println("  sin, cos, tan          三角函数");
        System.out.println("  asin, acos, atan       反三角函数");
        System.out.println("  sinh, cosh, tanh       双曲函数");
        System.out.println("  exp, ln, log, log10    指数和对数");
        System.out.println("  sqrt, cbrt, abs        根号和绝对值");
        System.out.println("  ceil, floor, round     取整函数");
        System.out.println("  signum, sign           符号函数 (-1, 0, 1)");
        System.out.println("  degrees, radians       角度 / 弧度转换");
        System.out.println("  atan2(y,x), hypot(x,y) 双参数反正切 / 斜边");
        System.out.println("  pow(base, exponent)    幂");
        System.out.println();

        System.out.println("  对于向量 X = [...]; Y = [...]; (也可手动按序展开输入):");
        System.out.println("  max(X), min(X)      多参数极值 (2+ 个)");
        System.out.println("  sum(X), count(X)    求和(1+ 个)、参数计数");
        System.out.println("  avg(X), median(X)   平均值 / 中位数 (1+ 个)");
        System.out.println("  prod(X), product(X) 乘积 (1+ 个)");
        System.out.println();

        System.out.println("  gcd(X), lcm(X)        最大公约数 / 最小公倍数 (整数, 2+ 个)");
        System.out.println("  range(X), geomean(X)  极差 / 几何平均数 (1+ 个正数)");
        System.out.println("  norm1(X), sumabs(X)   绝对值和 (L1)");
        System.out.println("  norm2(X), rms(X)      欧几里得范数 (L2) / 均方根");
        System.out.println("  var(X), variance(X)   样本方差 (≥2)");
        System.out.println("  std(X), stddev(X)     样本标准差 (≥2)");
        System.out.println("  varp(X), variancep(X) 总体方差 (≥1)");
        System.out.println("  stdp(X), stddevp(X)   总体标准差 (≥1)");
        System.out.println();

        System.out.println("  percentile(p, X), pctl(p, X)                百分位数(p ∈ [0,100])");
        System.out.println("  cov(X, Y), covariance(X, Y)                 样本协方差");
        System.out.println("  covp(X, Y), covariancep(X, Y)               总体协方差");
        System.out.println("  corr(X, Y), correlation(X, Y)               相关系数");
        System.out.println("  dot(X, Y), dotprod(X, Y)                    向量点积");
        System.out.println("  dist(X, Y), distance(X, Y), euclidean(X, Y) 欧几里得距离");
        System.out.println("  manhattan(X, Y), taxicab(X, Y)              曼哈顿距离");
        System.out.println();

        System.out.println("  transpose(matrix), t(matrix)     矩阵转置");
        System.out.println("  det(matrix), determinant(matrix) 行列式");
        System.out.println("  matmul(matrix_A, matrix_B)       矩阵乘法");
        System.out.println("  trace(matrix)                    矩阵的迹(主对角线之和)");
        System.out.println("  rank(matrix)                     矩阵的秩");
        System.out.println("  mean(matrix, axis)               矩阵均值 (axis=0 列, axis=1 行)");
        System.out.println("  inv(matrix)                      矩阵求逆");
        System.out.println("  solve(A, b)                      解线性方程组 Ax=b");
        System.out.println();

        System.out.println("【组合数学】");
        System.out.println("  C(n,k), comb(n,k)    组合数");
        System.out.println("  P(n,k), perm(n,k)    排列数");
        System.out.println();

        System.out.println("【变量】");
        System.out.println("  x = 10                 赋值");
        System.out.println("  x = 10; y = 2x         多语句 (分号分隔)");
        System.out.println("  ans                    上一次计算结果");

        System.out.println("【数组】");
        System.out.println("  [1, 2, 3]              数组字面量");
        System.out.println("  [[1,2], [3,4]]         二维数组 (矩阵)");
        System.out.println("  scores = [1, 2, 3]     数组变量赋值");
        System.out.println("  avg(scores)            数组作为函数参数");

        System.out.println("【隐式乘法】");
        System.out.println("  2PI, 3(4+5), 2sqrt(4)  自动识别乘法");

        System.out.println("【命令】");
        System.out.println("  help, ?        显示帮助");
        System.out.println("  vars           显示已定义的变量");
        System.out.println("  clear          清除所有变量");
        System.out.println("  lang           切换语言 (英文 ⇄ 中文)");
        System.out.println("  exit, quit, q  退出");
        System.out.println();
    }


    private static void printVariables() {
        if (context.size() <= 1) { // 只有ans变量
            System.out.println(getMessage("noVars"));
        } else { // 列出非ans变量
            System.out.println(getMessage("definedVars"));
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                if (!entry.getKey().equals("ans")) {
                    System.out.println("  " + entry.getKey() + " = " + entry.getValue().toString());
                }
            }
        }
    }
}


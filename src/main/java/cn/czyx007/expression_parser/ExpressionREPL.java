package cn.czyx007.expression_parser;

import cn.czyx007.expression_parser.ast.ExprNode;
import cn.czyx007.expression_parser.ast.Value;
import cn.czyx007.expression_parser.lexer.Lexer;
import cn.czyx007.expression_parser.parser.Parser;

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
                System.out.println("再见！");
                break;
            }

            // 帮助命令
            if (input.equalsIgnoreCase("help") || input.equals("?")) {
                printHelp();
                continue;
            }

            // 清除变量命令
            if (input.equalsIgnoreCase("clear")) {
                Object ansValue = context.getOrDefault("ans", new Value(0.0));
                context.clear();
                context.put("ans", ansValue);
                System.out.println("已清除所有变量。");
                continue;
            }

            // 显示变量命令
            if (input.equalsIgnoreCase("vars")) {
                printVariables();
                continue;
            }

            // 计算表达式
            try {
                Lexer lexer = new Lexer(input);
                Parser parser = new Parser(lexer);
                ExprNode root = parser.parse();
                Value result = root.evalValue(context);

                context.put("ans", result);
                System.out.println(result.toString());
            } catch (Exception e) {
                System.out.println("错误: " + e.getMessage());
            }
        }

        scanner.close();
    }

    private static void printWelcome() {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║       欢迎使用 Expression Parser 交互式计算器            ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  输入数学表达式进行计算，输入 help 查看帮助              ║");
        System.out.println("║  输入 exit 或 quit 退出                                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private static void printHelp() {
        System.out.println();
        System.out.println("=== 帮助信息 ===");
        System.out.println();
        System.out.println("【基本运算】");
        System.out.println("  +, -, *, /     加减乘除");
        System.out.println("  %              取模");
        System.out.println("  ^              幂运算 (右结合，如 2^3^2 = 512)");
        System.out.println("  !              阶乘 (如 5! = 120)");
        System.out.println();
        System.out.println("【常量】");
        System.out.println("  PI             圆周率 π ≈ 3.14159...");
        System.out.println("  E              自然常数 e ≈ 2.71828...");
        System.out.println();
        System.out.println("【函数】");
        System.out.println("  sin, cos, tan          三角函数");
        System.out.println("  asin, acos, atan       反三角函数");
        System.out.println("  sinh, cosh, tanh       双曲函数");
        System.out.println("  exp, ln, log, log10    指数和对数");
        System.out.println("  sqrt, cbrt, abs        根号和绝对值");
        System.out.println("  ceil, floor, round     取整函数");
        System.out.println("  signum(sign)           符号函数 (-1, 0, 1)");
        System.out.println("  degrees, radians       角度/弧度转换");
        System.out.println("  pow(x,y), hypot(x,y)   幂和斜边");
        System.out.println("  atan2(y,x)             双参数反正切");

        System.out.println("  max(...), min(...)      多参数极值 (2+个)");
        System.out.println("  sum(...), count(...)    求和(1+个)、参数计数");
        System.out.println("  avg(...), mean(...)     平均值 (1+个)");
        System.out.println("  prod(...), product(...) 乘积 (1+个)");
        System.out.println("  median(...)             中位数 (1+个)");

        System.out.println("  gcd(...), lcm(...)        最大公约数 / 最小公倍数 (支持 2+ 个整数参数)");
        System.out.println("  range(...), sumabs(...)   极差 / 绝对值和(L1) (1+个参数)");
        System.out.println("  norm2(...), rms(...)      欧几里得范数(L2) / 均方根 (1+个参数)");
        System.out.println("  geomean(...)              几何平均数 (1+个正数参数)");
        System.out.println("  var(...), variance(...)   样本方差 (至少2个参数)");
        System.out.println("  std(...), stddev(...)     样本标准差 (至少2个参数)");
        System.out.println("  varp(...), variancep(...) 总体方差 (至少1个参数)");
        System.out.println("  stdp(...), stddevp(...)   总体标准差 (至少1个参数)");
        System.out.println();
        System.out.println("【变量】");
        System.out.println("  x = 10                 赋值");
        System.out.println("  x = 10; y = 2x         多语句 (分号分隔)");
        System.out.println("  ans                    上一次计算结果");
        System.out.println();
        System.out.println("【数组】");
        System.out.println("  [1, 2, 3]              数组字面量");
        System.out.println("  [[1,2], [3,4]]         二维数组 (矩阵)");
        System.out.println("  scores = [1, 2, 3]     数组变量赋值");
        System.out.println("  avg(scores)            数组作为函数参数");
        System.out.println();
        System.out.println("【隐式乘法】");
        System.out.println("  2PI, 3(4+5), 2sqrt(4)  自动识别乘法");
        System.out.println();
        System.out.println("【命令】");
        System.out.println("  help, ?        显示帮助");
        System.out.println("  vars           显示已定义的变量");
        System.out.println("  clear          清除所有变量");
        System.out.println("  exit, quit, q  退出");
        System.out.println();
    }

    private static void printVariables() {
        if (context.size() <= 1) { // 只有ans变量
            System.out.println("当前没有定义任何变量。");
        } else {
            System.out.println("已定义的变量:");
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                if (!entry.getKey().equals("ans")) {
                    Object val = entry.getValue();
                    String valStr;
                    if (val instanceof Value) {
                        valStr = val.toString();
                    } else if (val instanceof Double) {
                        valStr = formatResult((Double) val);
                    } else {
                        valStr = String.valueOf(val);
                    }
                    System.out.println("  " + entry.getKey() + " = " + valStr);
                }
            }
        }
    }

    /**
     * 格式化结果：如果非常接近整数则显示整数，否则保留小数
     */
    private static String formatResult(double value) {
        if (Double.isNaN(value)) {
            return "NaN";
        }
        if (Double.isInfinite(value)) {
            return value > 0 ? "Infinity" : "-Infinity";
        }
        if (value == Math.floor(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        // 移除不必要的尾随零
        return String.format("%.15g", value);
    }
}


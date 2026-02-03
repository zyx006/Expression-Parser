package cn.czyx007.expression_parser.utils;

import cn.czyx007.expression_parser.ast.Value;
import cn.czyx007.expression_parser.exception.ErrorCode;
import cn.czyx007.expression_parser.exception.ExpressionException;

import java.util.*;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

import static cn.czyx007.expression_parser.utils.MatrixMathUtils.*;
import static cn.czyx007.expression_parser.utils.ScalarMathUtils.*;

/**
 * 函数注册中心<br/>
 * 管理标量函数和矩阵函数的注册与调用
 */
public final class FunctionRegistry {

    /**
     * 函数式接口：标量数学函数<br/>
     * 参数为展开后的 double...，数组会被 flatten
     */
    @FunctionalInterface
    public interface MathFunction {
        double apply(double... args);
    }

    /**
     * 函数式接口：矩阵函数<br/>
     * 参数为原始 Value，不进行数组展开
     */
    @FunctionalInterface
    public interface MatrixFunction {
        Value apply(List<Value> args);
    }

    /** 标量函数注册表 */
    public static final Map<String, MathFunction> FUNCTION_REGISTRY = new HashMap<>();
    /** 矩阵函数注册表 */
    public static final Map<String, MatrixFunction> MATRIX_FUNCTION_REGISTRY = new HashMap<>();

    static {
        // 单参数函数 - 三角函数
        register1("sin", Math::sin);
        register1("cos", Math::cos);
        register1("tan", Math::tan);
        register1("asin", Math::asin);
        register1("acos", Math::acos);
        register1("atan", Math::atan);

        // 单参数函数 - 双曲函数
        register1("sinh", Math::sinh);
        register1("cosh", Math::cosh);
        register1("tanh", Math::tanh);

        // 单参数函数 - 指数和对数
        register1("exp", Math::exp);
        register1("ln", x -> { validate(x > 0, ErrorCode.PARAM_MUST_BE_POSITIVE.formatMessage("ln")); return Math.log(x); });
        register1("log10", x -> { validate(x > 0, ErrorCode.PARAM_MUST_BE_POSITIVE.formatMessage("log10")); return Math.log10(x); });

        // 单参数函数 - 根号和立方根
        register1("sqrt", x -> { validate(x >= 0, ErrorCode.SQRT_NEGATIVE.formatMessage()); return Math.sqrt(x); });
        register1("cbrt", Math::cbrt);

        // 单参数函数 - 取整
        register1("abs", Math::abs);
        register1("ceil", Math::ceil);
        register1("floor", Math::floor);
        register1("round", x -> (double) Math.round(x));

        // 单参数函数 - 其他
        register1("signum", Math::signum);
        register1("sign", Math::signum);  // signum 的别名

        // 单参数函数 - 角度转换
        register1("degrees", Math::toDegrees);
        register1("radians", Math::toRadians);


        // 双参数函数
        register2("pow", Math::pow);
        register2("hypot", Math::hypot);
        register2("atan2", Math::atan2);


        // 可变参数函数 - max/min 支持 2+ 个参数
        registerN("max", args -> {
            validateMinArgs("max", args.length, 2);
            double result = args[0];
            for (int i = 1; i < args.length; i++) {
                result = Math.max(result, args[i]);
            }
            return result;
        });

        registerN("min", args -> {
            validateMinArgs("min", args.length, 2);
            double result = args[0];
            for (int i = 1; i < args.length; i++) {
                result = Math.min(result, args[i]);
            }
            return result;
        });

        // gcd: 支持 2+ 个整数参数，依次计算最大公约数
        registerN("gcd", args -> {
            validateMinArgs("gcd", args.length, 2);
            long result = requireInteger(args[0], "gcd");
            for (int i = 1; i < args.length; i++) {
                result = gcd(result, requireInteger(args[i], "gcd"));
            }
            return result;
        });

        // lcm: 支持 2+ 个整数参数，依次计算最小公倍数
        registerN("lcm", args -> {
            validateMinArgs("lcm", args.length, 2);
            long result = requireInteger(args[0], "lcm");
            for (int i = 1; i < args.length; i++) {
                result = lcm(result, requireInteger(args[i], "lcm"));
            }
            return result;
        });


        // 可变参数函数 - 统计函数
        // 求和
        registerN("sum", args -> {
            validateMinArgs("sum", args.length, 1);
            double sum = 0;
            for (double arg : args) sum += arg;
            return sum;
        });

        // 平均值
        registerN("avg", args -> {
            validateMinArgs("avg", args.length, 1);
            double sum = 0;
            for (double arg : args) sum += arg;
            return sum / args.length;
        });

        // 乘积
        registerN("prod", args -> {
            validateMinArgs("prod", args.length, 1);
            double product = 1;
            for (double arg : args) product *= arg;
            return product;
        });
        registerAlias("product", "prod");

        // 计数
        registerN("count", args -> args.length);

        // 中位数
        registerN("median", args -> {
            validateMinArgs("median", args.length, 1);
            double[] sorted = args.clone();
            java.util.Arrays.sort(sorted);
            int n = sorted.length;
            if (n % 2 == 0) {
                return (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0;
            } else {
                return sorted[n / 2];
            }
        });

        // range极差: 支持 1+ 个参数，返回最大值与最小值之差
        registerN("range", args -> {
            validateMinArgs("range", args.length, 1);
            double min = args[0], max = args[0];
            for (int i = 1; i < args.length; i++) {
                min = Math.min(min, args[i]);
                max = Math.max(max, args[i]);
            }
            return max - min;
        });

        // sumabs: 支持 1+ 个参数，返回绝对值之和（L1）
        registerN("sumabs", args -> {
            validateMinArgs("sumabs", args.length, 1);
            double s = 0;
            for (double x : args) s += Math.abs(x);
            return s;
        });
        registerAlias("norm1", "sumabs");

        // norm2: 支持 1+ 个参数，返回欧几里得范数（L2）
        registerN("norm2", args -> {
            validateMinArgs("norm2", args.length, 1);
            double s = 0;
            for (double x : args) s += x * x;
            return Math.sqrt(s);
        });

        // rms: 支持 1+ 个参数，返回均方根
        registerN("rms", args -> {
            validateMinArgs("rms", args.length, 1);
            double s = 0;
            for (double x : args) s += x * x;
            return Math.sqrt(s / args.length);
        });

        // geomean: 支持 1+ 个参数，返回几何平均数
        registerN("geomean", args -> {
            validateMinArgs("geomean", args.length, 1);
            double prod = 1;
            for (double x : args) {
                validate(x > 0, ErrorCode.GEOMEAN_POSITIVE.formatMessage());
                prod *= x;
            }
            return Math.pow(prod, 1.0 / args.length);
        });

        // 样本方差
        registerN("var", args -> variance(args, true));
        registerAlias("variance", "var");

        // 样本标准差
        registerN("std", args -> Math.sqrt(variance(args, true)));
        registerAlias("stddev", "std");

        // 总体方差
        registerN("varp", args -> variance(args, false));
        registerAlias("variancep", "varp");

        // 总体标准差
        registerN("stdp", args -> Math.sqrt(variance(args, false)));
        registerAlias("stddevp", "stdp");

        // 百分位数：percentile(p, x1, x2, ...) 或 percentile(p, array)
        // p 为百分位数（0-100），例如 50 表示中位数
        registerN("percentile", args -> {
            validateMinArgs("percentile", args.length, 2);
            double p = args[0];
            validate(p >= 0 && p <= 100, ErrorCode.PERCENTILE_RANGE.formatMessage());

            // 提取数据点（跳过第一个参数）
            double[] data = new double[args.length - 1];
            System.arraycopy(args, 1, data, 0, args.length - 1);
            java.util.Arrays.sort(data);

            int n = data.length;
            if (n == 1) return data[0];

            // 使用线性插值法计算百分位数
            double index = (p / 100.0) * (n - 1);
            int lower = (int) Math.floor(index);
            int upper = (int) Math.ceil(index);

            if (lower == upper) return data[lower];

            double weight = index - lower;
            return data[lower] * (1 - weight) + data[upper] * weight;
        });
        registerAlias("pctl", "percentile");

        // 协方差：cov(x1, x2, x3, ..., y1, y2, y3, ...)
        // 参数必须是偶数个，前半部分为 X，后半部分为 Y
        registerN("cov", args -> {
            validateMinArgs("cov", args.length, 2);
            validate(args.length % 2 == 0, ErrorCode.COV_EVEN_ARGS.formatMessage());

            int n = args.length / 2;
            validate(n >= 2, ErrorCode.COV_MIN_PAIRS.formatMessage(2));

            double[] x = new double[n];
            double[] y = new double[n];
            System.arraycopy(args, 0, x, 0, n);
            System.arraycopy(args, n, y, 0, n);

            return covariance(x, y, true);
        });
        registerAlias("covariance", "cov");

        // 总体协方差
        registerN("covp", args -> {
            validateMinArgs("covp", args.length, 2);
            validate(args.length % 2 == 0, ErrorCode.COV_EVEN_ARGS.formatMessage());

            int n = args.length / 2;
            validate(n >= 1, ErrorCode.COV_MIN_PAIRS.formatMessage(1));

            double[] x = new double[n];
            double[] y = new double[n];
            System.arraycopy(args, 0, x, 0, n);
            System.arraycopy(args, n, y, 0, n);

            return covariance(x, y, false);
        });
        registerAlias("covariancep", "covp");

        // 相关系数：corr(x1, x2, x3, ..., y1, y2, y3, ...)
        // 参数必须是偶数个，前半部分为 X，后半部分为 Y
        registerN("corr", args -> {
            validateMinArgs("corr", args.length, 2);
            validate(args.length % 2 == 0, ErrorCode.CORR_EVEN_ARGS.formatMessage());

            int n = args.length / 2;
            validate(n >= 2, ErrorCode.CORR_MIN_PAIRS.formatMessage());

            double[] x = new double[n];
            double[] y = new double[n];
            System.arraycopy(args, 0, x, 0, n);
            System.arraycopy(args, n, y, 0, n);

            return correlation(x, y);
        });
        registerAlias("correlation", "corr");

        // 点积：dot(x1, x2, x3, ..., y1, y2, y3, ...)
        // 参数必须是偶数个，前半部分为向量 X，后半部分为向量 Y
        registerN("dot", args -> {
            validateMinArgs("dot", args.length, 2);
            validate(args.length % 2 == 0, ErrorCode.DOT_EVEN_ARGS.formatMessage());

            int n = args.length / 2;
            double result = 0;
            for (int i = 0; i < n; i++) {
                result += args[i] * args[n + i];
            }
            return result;
        });
        registerAlias("dotprod", "dot");

        // 欧几里得距离：dist(x1, x2, x3, ..., y1, y2, y3, ...)
        // 参数必须是偶数个，前半部分为点 X，后半部分为点 Y
        registerN("dist", args -> {
            validateMinArgs("dist", args.length, 2);
            validate(args.length % 2 == 0, ErrorCode.DIST_EVEN_ARGS.formatMessage());

            int n = args.length / 2;
            double sumSq = 0;
            for (int i = 0; i < n; i++) {
                double diff = args[i] - args[n + i];
                sumSq += diff * diff;
            }
            return Math.sqrt(sumSq);
        });
        registerAlias("distance", "dist");
        registerAlias("euclidean", "dist");

        // 曼哈顿距离：manhattan(x1, x2, x3, ..., y1, y2, y3, ...)
        registerN("manhattan", args -> {
            validateMinArgs("manhattan", args.length, 2);
            validate(args.length % 2 == 0, ErrorCode.MANHATTAN_EVEN_ARGS.formatMessage());

            int n = args.length / 2;
            double sum = 0;
            for (int i = 0; i < n; i++) {
                sum += Math.abs(args[i] - args[n + i]);
            }
            return sum;
        });
        registerAlias("taxicab", "manhattan");


        // 特殊函数：log 支持 1 或 2 个参数
        FUNCTION_REGISTRY.put("log", args -> {
            if (args.length == 1) {
                validate(args[0] > 0, ErrorCode.LOG_PARAM_INVALID.formatMessage());
                return Math.log10(args[0]);
            } else if (args.length == 2) {
                validate(args[0] > 0 && args[0] != 1, ErrorCode.LOG_BASE_INVALID.formatMessage());
                validate(args[1] > 0, ErrorCode.LOG_PARAM_INVALID.formatMessage());
                return Math.log(args[1]) / Math.log(args[0]);
            } else {
                throw new ExpressionException(ErrorCode.LOG_INVALID_ARGS, args.length);
            }
        });

        // ========== 矩阵函数 ==========
        // 矩阵转置：transpose([[1,2],[3,4]]) => [[1,3],[2,4]]
        registerMatrix("transpose", args -> {
            validateArgCount("transpose", args.size(), 1);
            return transposeMatrix(args.get(0));
        });
        registerMatrixAlias("t", "transpose");

        // 行列式：det([[1,2],[3,4]])
        registerMatrix("det", args -> {
            validateArgCount("det", args.size(), 1);
            return new Value(determinant(args.get(0)));
        });
        registerMatrixAlias("determinant", "det");

        // 矩阵乘法
        registerMatrix("matmul", args -> {
            validateArgCount("matmul", args.size(), 2);
            return matMul(args.get(0), args.get(1));
        });

        // 矩阵迹
        registerMatrix("trace", args -> {
            validateArgCount("trace", args.size(), 1);
            return new Value(trace(args.get(0)));
        });

        // 矩阵秩
        registerMatrix("rank", args -> {
            validateArgCount("rank", args.size(), 1);
            return new Value(matrixRank(args.get(0)));
        });

        // mean(A, axis)
        // axis = 0 : 按列求均值，返回 1×n 行向量
        // axis = 1 : 按行求均值，返回 m×1 列向量
        registerMatrix("mean", args -> {
            validateArgCount("mean", args.size(), 2);
            Value axisVal = args.get(1);
            if (!axisVal.isScalar()) {
                throw new ExpressionException(ErrorCode.SCALAR_REQUIRED, "mean");
            }
            int axis = (int) axisVal.asScalar();
            return meanMatrix(args.get(0), axis);
        });

        // ========== 矩阵求逆 ==========
        registerMatrix("inv", args -> {
            validateArgCount("inv", args.size(), 1);
            return inverseMatrix(args.get(0));
        });

        // ========== 解线性方程组 solve(A, b) ==========
        registerMatrix("solve", args -> {
            validateArgCount("solve", args.size(), 2);
            return solveLinear(args.get(0), args.get(1));
        });

        // ========== 排列组合 ==========
        // 组合数 C(n,k)
        registerN("c", args -> {
            validateArgCount("C", args.length, 2);
            int n = (int) args[0];
            int k = (int) args[1];
            validate(n >= 0 && k >= 0, ErrorCode.COMB_NON_NEGATIVE.formatMessage());
            if (k > n) return 0;
            if (k > n - k) k = n - k;
            double result = 1;
            for (int i = 1; i <= k; i++) {
                result = result * (n - k + i) / i;
            }
            return result;
        });
        registerAlias("comb", "c");

        // 排列数 P(n,k)
        registerN("p", args -> {
            validateArgCount("P", args.length, 2);
            int n = (int) args[0];
            int k = (int) args[1];
            validate(n >= 0 && k >= 0, ErrorCode.PERM_NON_NEGATIVE.formatMessage());
            if (k > n) return 0;
            double result = 1;
            for (int i = 0; i < k; i++) {
                result *= (n - i);
            }
            return result;
        });
        registerAlias("perm", "p");
    }


    /**
     * 注册单参数函数
     * @param name 函数名
     * @param func 函数实现
     */
    static void register1(String name, DoubleUnaryOperator func) {
        FUNCTION_REGISTRY.put(name, args -> {
            validateArgCount(name, args.length, 1);
            return func.applyAsDouble(args[0]);
        });
    }

    /**
     * 注册双参数函数
     * @param name 函数名
     * @param func 函数实现
     */
    static void register2(String name, DoubleBinaryOperator func) {
        FUNCTION_REGISTRY.put(name, args -> {
            validateArgCount(name, args.length, 2);
            return func.applyAsDouble(args[0], args[1]);
        });
    }

    /**
     * 注册可变参数函数
     * @param name 函数名
     * @param func 函数实现
     */
    static void registerN(String name, MathFunction func) {
        FUNCTION_REGISTRY.put(name, func);
    }

    /**
     * 注册函数别名
     * @param alias 别名
     * @param originalName 原函数名
     */
    static void registerAlias(String alias, String originalName) {
        MathFunction original = FUNCTION_REGISTRY.get(originalName);
        if (original == null) {
            throw new ExpressionException(ErrorCode.ALIAS_NOT_FOUND, alias, originalName);
        }
        FUNCTION_REGISTRY.put(alias, original);
    }

    /**
     * 注册矩阵函数
     * @param name 函数名
     * @param func 函数实现
     */
    static void registerMatrix(String name, MatrixFunction func) {
        MATRIX_FUNCTION_REGISTRY.put(name, func);
    }

    /**
     * 注册矩阵函数别名
     * @param alias 别名
     * @param originalName 原函数名
     */
    static void registerMatrixAlias(String alias, String originalName) {
        MatrixFunction original = MATRIX_FUNCTION_REGISTRY.get(originalName);
        if (original == null) {
            throw new ExpressionException(ErrorCode.MATRIX_ALIAS_NOT_FOUND, alias, originalName);
        }
        MATRIX_FUNCTION_REGISTRY.put(alias, original);
    }


    /**
     * 验证参数数量（精确匹配）
     * @param funcName 函数名
     * @param actual 实际参数数量
     * @param expected 期望参数数量
     */
    static void validateArgCount(String funcName, int actual, int expected) {
        if (actual != expected) {
            throw new ExpressionException(ErrorCode.INVALID_ARG_COUNT, funcName, expected, actual);
        }
    }

    /**
     * 验证参数数量（最小值）
     * @param funcName 函数名
     * @param actual 实际参数数量
     * @param min 最小参数数量
     */
    static void validateMinArgs(String funcName, int actual, int min) {
        if (actual < min) {
            throw new ExpressionException(ErrorCode.INVALID_MIN_ARG_COUNT, funcName, min, actual);
        }
    }

    /**
     * 条件验证
     * @param condition 验证条件
     * @param formattedMessage 格式化后的错误消息
     */
    static void validate(boolean condition, String formattedMessage) {
        if (!condition) {
            throw new ExpressionException(ErrorCode.VALIDATION_ERROR, formattedMessage);
        }
    }
}

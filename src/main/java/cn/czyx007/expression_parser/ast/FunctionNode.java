package cn.czyx007.expression_parser.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;
import java.util.function.DoubleBinaryOperator;

/**
 * 函数调用节点 - 支持数学函数如 sin, cos, sqrt 等
 * 使用函数注册表实现，易于维护和扩展
 */
public class FunctionNode extends ExprNode {
    private final String funcName;
    private final List<ExprNode> args;

    // 函数式接口：可变参数函数
    @FunctionalInterface
    interface MathFunction {
        double apply(double... args);
    }

    // 函数式接口：矩阵函数（处理 Value 对象）
    @FunctionalInterface
    interface MatrixFunction {
        Value apply(List<Value> args);
    }

    // ========== 构造函数 ==========
    public FunctionNode(String funcName, List<ExprNode> args) {
        this.funcName = funcName.toLowerCase(); // 函数名不区分大小写
        this.args = args;
    }

    // ========== 函数注册表 ==========
    private static final Map<String, MathFunction> FUNCTION_REGISTRY = new HashMap<>();
    private static final Map<String, MatrixFunction> MATRIX_FUNCTION_REGISTRY = new HashMap<>();

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
        register1("ln", x -> { validate(x > 0, "ln 的参数必须大于 0"); return Math.log(x); });
        register1("log10", x -> { validate(x > 0, "log10 的参数必须大于 0"); return Math.log10(x); });

        // 单参数函数 - 根号和立方根
        register1("sqrt", x -> { validate(x >= 0, "sqrt 的参数不能为负数"); return Math.sqrt(x); });
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
        registerAlias("mean", "avg");

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
                validate(x > 0, "geomean 的参数必须大于 0");
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
            validate(p >= 0 && p <= 100, "百分位数必须在 0 到 100 之间");

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
            validate(args.length % 2 == 0, "协方差需要偶数个参数（前半部分为 X，后半部分为 Y）");

            int n = args.length / 2;
            validate(n >= 2, "协方差计算至少需要 2 对数据点");

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
            validate(args.length % 2 == 0, "总体协方差需要偶数个参数（前半部分为 X，后半部分为 Y）");

            int n = args.length / 2;
            validate(n >= 1, "总体协方差计算至少需要 1 对数据点");

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
            validate(args.length % 2 == 0, "相关系数需要偶数个参数（前半部分为 X，后半部分为 Y）");

            int n = args.length / 2;
            validate(n >= 2, "相关系数计算至少需要 2 对数据点");

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
            validate(args.length % 2 == 0, "点积需要偶数个参数（前半部分为向量 X，后半部分为向量 Y）");

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
            validate(args.length % 2 == 0, "距离计算需要偶数个参数（前半部分为点 X，后半部分为点 Y）");

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
            validate(args.length % 2 == 0, "曼哈顿距离需要偶数个参数（前半部分为点 X，后半部分为点 Y）");

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
                validate(args[0] > 0, "log 的参数必须大于 0");
                return Math.log10(args[0]);
            } else if (args.length == 2) {
                validate(args[0] > 0 && args[0] != 1, "log 的底数必须大于 0 且不等于 1");
                validate(args[1] > 0, "log 的参数必须大于 0");
                return Math.log(args[1]) / Math.log(args[0]);
            } else {
                throw new RuntimeException("函数 log 需要 1 或 2 个参数，但得到 " + args.length + " 个");
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
    }

    // 辅助方法：注册单参数函数
    private static void register1(String name, DoubleUnaryOperator func) {
        FUNCTION_REGISTRY.put(name, args -> {
            validateArgCount(name, args.length, 1);
            return func.applyAsDouble(args[0]);
        });
    }

    // 辅助方法：注册双参数函数
    private static void register2(String name, DoubleBinaryOperator func) {
        FUNCTION_REGISTRY.put(name, args -> {
            validateArgCount(name, args.length, 2);
            return func.applyAsDouble(args[0], args[1]);
        });
    }

    // 辅助方法：注册可变参数函数（直接使用 MathFunction）
    private static void registerN(String name, MathFunction func) {
        FUNCTION_REGISTRY.put(name, func);
    }

    // 辅助方法：注册函数别名
    private static void registerAlias(String alias, String originalName) {
        MathFunction original = FUNCTION_REGISTRY.get(originalName);
        if (original == null) {
            throw new IllegalStateException("无法为不存在的函数 '" + originalName + "' 创建别名 '" + alias + "'");
        }
        FUNCTION_REGISTRY.put(alias, original);
    }

    // 辅助方法：注册矩阵函数
    private static void registerMatrix(String name, MatrixFunction func) {
        MATRIX_FUNCTION_REGISTRY.put(name, func);
    }

    // 注册矩阵函数别名
    private static void registerMatrixAlias(String alias, String originalName) {
        MatrixFunction original = MATRIX_FUNCTION_REGISTRY.get(originalName);
        if (original == null) {
            throw new IllegalStateException(
                    "无法为不存在的矩阵函数 '" + originalName + "' 创建别名 '" + alias + "'"
            );
        }
        MATRIX_FUNCTION_REGISTRY.put(alias, original);
    }

    // 辅助方法：计算最大公约数
    private static long gcd(long a, long b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) {
            long t = a % b;
            a = b;
            b = t;
        }
        return a;
    }

    // 辅助方法：计算最小公倍数
    private static long lcm(long a, long b) {
        return Math.abs(a / gcd(a, b) * b);
    }

    // 辅助方法：确保参数为整数
    private static long requireInteger(double x, String func) {
        if (x != Math.rint(x)) {
            throw new ArithmeticException(func + " 的参数必须是整数");
        }
        return (long) x;
    }

    // 辅助方法：计算总体或样本方差
    private static double variance(double[] args, boolean sample) {
        int n = args.length;
        if (sample && n < 2) {
            throw new RuntimeException("样本方差至少需要 2 个参数");
        }
        if (!sample && n < 1) {
            throw new RuntimeException("总体方差至少需要 1 个参数");
        }

        // 计算均值
        double mean = 0;
        for (double x : args) mean += x;
        mean /= n;

        // 计算平方差之和
        double sumSq = 0;
        for (double x : args) {
            double d = x - mean;
            sumSq += d * d;
        }

        //样本方差除以 n-1，总体方差除以 n
        return sumSq / (sample ? (n - 1) : n);
    }

    // 辅助方法：计算协方差
    private static double covariance(double[] x, double[] y, boolean sample) {
        int n = x.length;
        if (sample && n < 2) {
            throw new RuntimeException("样本协方差至少需要 2 对数据点");
        }
        if (!sample && n < 1) {
            throw new RuntimeException("总体协方差至少需要 1 对数据点");
        }

        // 计算均值
        double meanX = 0, meanY = 0;
        for (int i = 0; i < n; i++) {
            meanX += x[i];
            meanY += y[i];
        }
        meanX /= n;
        meanY /= n;

        // 计算协方差
        double cov = 0;
        for (int i = 0; i < n; i++) {
            cov += (x[i] - meanX) * (y[i] - meanY);
        }

        return cov / (sample ? (n - 1) : n);
    }

    // 辅助方法：计算相关系数
    private static double correlation(double[] x, double[] y) {
        int n = x.length;
        if (n < 2) {
            throw new RuntimeException("相关系数计算至少需要 2 对数据点");
        }

        double cov = covariance(x, y, true);
        double stdX = Math.sqrt(variance(x, true));
        double stdY = Math.sqrt(variance(y, true));

        if (stdX == 0 || stdY == 0) {
            throw new ArithmeticException("标准差为 0，无法计算相关系数");
        }

        return cov / (stdX * stdY);
    }

    // 辅助方法：矩阵转置
    private static Value transposeMatrix(Value matrix) {
        if (!matrix.isArray()) {
            throw new RuntimeException("transpose 需要一个数组参数");
        }

        List<Value> rows = matrix.asArray();
        if (rows.isEmpty()) {
            return new Value(new ArrayList<>());
        }

        // 检查是否为二维矩阵
        if (!rows.get(0).isArray()) {
            throw new RuntimeException("transpose 需要一个二维矩阵");
        }

        int numRows = rows.size();
        int numCols = rows.get(0).asArray().size();

        // 验证所有行的列数相同
        for (Value row : rows) {
            if (!row.isArray() || row.asArray().size() != numCols) {
                throw new RuntimeException("矩阵的所有行必须具有相同的列数");
            }
        }

        // 执行转置
        List<Value> transposed = new ArrayList<>();
        for (int col = 0; col < numCols; col++) {
            List<Value> newRow = new ArrayList<>();
            for (int row = 0; row < numRows; row++) {
                newRow.add(rows.get(row).asArray().get(col));
            }
            transposed.add(new Value(newRow));
        }

        return new Value(transposed);
    }

    // 辅助方法：计算行列式
    private static double determinant(Value matrix) {
        if (!matrix.isArray()) {
            throw new RuntimeException("det 需要一个数组参数");
        }

        List<Value> rows = matrix.asArray();
        if (rows.isEmpty()) {
            throw new RuntimeException("不能计算空矩阵的行列式");
        }

        // 将 Value 矩阵转换为 double[][]
        int n = rows.size();

        // 检查第一行
        if (!rows.get(0).isArray()) {
            throw new RuntimeException("det 需要一个二维方阵");
        }

        int m = rows.get(0).asArray().size();
        if (n != m) {
            throw new RuntimeException("det 需要一个方阵（行数必须等于列数）");
        }

        // 转换为 double[][]
        double[][] mat = new double[n][n];
        for (int i = 0; i < n; i++) {
            if (!rows.get(i).isArray()) {
                throw new RuntimeException("矩阵格式错误");
            }
            List<Value> row = rows.get(i).asArray();
            if (row.size() != n) {
                throw new RuntimeException("det 需要一个方阵（所有行必须具有相同的列数）");
            }
            for (int j = 0; j < n; j++) {
                if (!row.get(j).isScalar()) {
                    throw new RuntimeException("矩阵元素必须是标量");
                }
                mat[i][j] = row.get(j).asScalar();
            }
        }

        return calculateDeterminant(mat, n);
    }

    // 辅助方法：递归计算行列式（使用拉普拉斯展开）
    private static double calculateDeterminant(double[][] mat, int n) {
        if (n == 1) {
            return mat[0][0];
        }
        if (n == 2) {
            return mat[0][0] * mat[1][1] - mat[0][1] * mat[1][0];
        }

        double det = 0;

        // 使用第一行进行拉普拉斯展开
        for (int col = 0; col < n; col++) {
            // 计算余子式
            double[][] subMat = new double[n - 1][n - 1];
            for (int i = 1; i < n; i++) {
                int subCol = 0;
                for (int j = 0; j < n; j++) {
                    if (j != col) {
                        subMat[i - 1][subCol] = mat[i][j];
                        subCol++;
                    }
                }
            }

            // 计算代数余子式
            double cofactor = Math.pow(-1, col) * mat[0][col] * calculateDeterminant(subMat, n - 1);
            det += cofactor;
        }

        return det;
    }

    // 参数数量验证（精确匹配）
    private static void validateArgCount(String funcName, int actual, int expected) {
        if (actual != expected) {
            throw new RuntimeException("函数 " + funcName + " 需要 " + expected + " 个参数，但得到 " + actual + " 个");
        }
    }

    // 参数数量验证（最小值）
    private static void validateMinArgs(String funcName, int actual, int min) {
        if (actual < min) {
            throw new RuntimeException("函数 " + funcName + " 至少需要 " + min + " 个参数，但得到 " + actual + " 个");
        }
    }

    // 条件验证
    private static void validate(boolean condition, String message) {
        if (!condition) {
            throw new ArithmeticException(message);
        }
    }

    // ========== 求值逻辑 ==========
    @Override
    public double eval(Map<String, Double> context) {
        // 从注册表中查找函数
        MathFunction func = FUNCTION_REGISTRY.get(funcName);
        if (func == null) {
            throw new RuntimeException("未知的函数: " + funcName);
        }

        // 计算所有参数
        double[] argValues = new double[args.size()];
        for (int i = 0; i < args.size(); i++) {
            argValues[i] = args.get(i).eval(context);
        }

        // 调用函数并返回结果
        return fixPrecision(func.apply(argValues));
    }

    @Override
    public Value evalValue(Map<String, Object> context) {
        // 首先检查是否是矩阵函数
        MatrixFunction matrixFunc = MATRIX_FUNCTION_REGISTRY.get(funcName);
        if (matrixFunc != null) {
            // 矩阵函数：不展开数组，保留结构
            List<Value> argValues = new ArrayList<>();
            for (ExprNode arg : args) {
                argValues.add(arg.evalValue(context));
            }
            return matrixFunc.apply(argValues);
        }

        // 普通函数：从注册表中查找
        MathFunction func = FUNCTION_REGISTRY.get(funcName);
        if (func == null) {
            throw new RuntimeException("未知的函数: " + funcName);
        }

        // 收集所有参数值（展开数组）
        List<Double> allValues = new ArrayList<>();
        for (ExprNode arg : args) {
            Value v = arg.evalValue(context);
            v.collectScalars(allValues);
        }

        // 转换为 double 数组
        double[] argValues = new double[allValues.size()];
        for (int i = 0; i < allValues.size(); i++) {
            argValues[i] = allValues.get(i);
        }

        // 调用函数并返回结果
        return new Value(fixPrecision(func.apply(argValues)));
    }
}

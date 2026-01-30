package cn.czyx007.expression_parser.utils;

/**
 * 标量数学与统计工具
 */
final class ScalarMathUtils {

    // 辅助方法：计算最大公约数
    static long gcd(long a, long b) {
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
    static long lcm(long a, long b) {
        return Math.abs(a / gcd(a, b) * b);
    }

    // 辅助方法：确保参数为整数
    static long requireInteger(double x, String func) {
        if (x != Math.rint(x)) {
            throw new ArithmeticException(func + " 的参数必须是整数");
        }
        return (long) x;
    }


    // 辅助方法：计算总体或样本方差
    static double variance(double[] args, boolean sample) {
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
    static double covariance(double[] x, double[] y, boolean sample) {
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
    static double correlation(double[] x, double[] y) {
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
}

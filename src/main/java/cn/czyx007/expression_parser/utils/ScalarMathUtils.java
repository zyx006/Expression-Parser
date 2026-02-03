package cn.czyx007.expression_parser.utils;

import cn.czyx007.expression_parser.exception.ErrorCode;
import cn.czyx007.expression_parser.exception.ExpressionException;

/**
 * 标量数学与统计工具类<br/>
 * 提供基础数学运算和统计函数的辅助方法
 */
final class ScalarMathUtils {

    /**
     * 计算最大公约数（GCD）
     * @param a 第一个数
     * @param b 第二个数
     * @return 最大公约数
     */
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

    /**
     * 计算最小公倍数（LCM）
     * @param a 第一个数
     * @param b 第二个数
     * @return 最小公倍数
     */
    static long lcm(long a, long b) {
        return Math.abs(a / gcd(a, b) * b);
    }

    /**
     * 确保参数为整数
     * @param x 待检查的值
     * @param func 函数名（用于错误消息）
     * @return 整数值
     */
    static long requireInteger(double x, String func) {
        if (x != Math.rint(x)) {
            throw new ExpressionException(ErrorCode.INTEGER_REQUIRED, func);
        }
        return (long) x;
    }


    /**
     * 计算方差（总体或样本）
     * @param args 数据数组
     * @param sample true 表示样本方差，false 表示总体方差
     * @return 方差值
     */
    static double variance(double[] args, boolean sample) {
        int n = args.length;
        if (sample && n < 2) {
            throw new ExpressionException(ErrorCode.VARIANCE_MIN_ARGS);
        }
        if (!sample && n < 1) {
            throw new ExpressionException(ErrorCode.VARIANCE_POP_MIN_ARGS);
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

    /**
     * 计算协方差（总体或样本）
     * @param x X 数据数组
     * @param y Y 数据数组
     * @param sample true 表示样本协方差，false 表示总体协方差
     * @return 协方差值
     */
    static double covariance(double[] x, double[] y, boolean sample) {
        int n = x.length;
        if (sample && n < 2) {
            throw new ExpressionException(ErrorCode.COVARIANCE_MIN_ARGS);
        }
        if (!sample && n < 1) {
            throw new ExpressionException(ErrorCode.COVARIANCE_POP_MIN_ARGS);
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

    /**
     * 计算相关系数（Pearson 相关系数）
     * @param x X 数据数组
     * @param y Y 数据数组
     * @return 相关系数
     */
    static double correlation(double[] x, double[] y) {
        int n = x.length;
        if (n < 2) {
            throw new ExpressionException(ErrorCode.CORRELATION_MIN_ARGS);
        }

        double cov = covariance(x, y, true);
        double stdX = Math.sqrt(variance(x, true));
        double stdY = Math.sqrt(variance(y, true));

        if (stdX == 0 || stdY == 0) {
            throw new ExpressionException(ErrorCode.STD_DEV_ZERO);
        }

        return cov / (stdX * stdY);
    }
}

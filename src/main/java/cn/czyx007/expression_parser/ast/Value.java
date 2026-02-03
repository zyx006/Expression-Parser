package cn.czyx007.expression_parser.ast;

import cn.czyx007.expression_parser.exception.ErrorCode;
import cn.czyx007.expression_parser.exception.ExpressionException;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;

/**
 * 值类型 - 支持标量（double）或数组（List<Value>）
 * 用于支持多数值变量和矩阵运算
 */
public class Value {
    private final Double scalar;
    private final List<Value> array;

    private static final double EPS = 1e-12;

    private static final DecimalFormat DF;

    static {
        DF = new DecimalFormat("0.###############");
        DF.setRoundingMode(RoundingMode.HALF_UP);
    }

    // 标量构造
    public Value(double scalar) {
        this.scalar = scalar;
        this.array = null;
    }

    // 数组构造
    public Value(List<Value> array) {
        this.scalar = null;
        this.array = array;
    }

    public boolean isScalar() {
        return scalar != null;
    }

    public boolean isArray() {
        return array != null;
    }

    public double asScalar() {
        if (!isScalar()) {
            throw new ExpressionException(ErrorCode.ARRAY_TO_SCALAR_ERROR);
        }
        return scalar;
    }

    public List<Value> asArray() {
        if (!isArray()) {
            throw new ExpressionException(ErrorCode.SCALAR_TO_ARRAY_ERROR);
        }
        return array;
    }

    /**
     * 将数组展平为 double 数组（仅支持一维数组）
     */
    public double[] flattenToDoubleArray() {
        if (isScalar()) {
            return new double[]{scalar};
        }
        // 检查是否为一维数组（所有元素都是标量）
        if (array.stream().allMatch(Value::isScalar)) {
            double[] result = new double[array.size()];
            for (int i = 0; i < array.size(); i++) {
                result[i] = array.get(i).asScalar();
            }
            return result;
        }
        throw new ExpressionException(ErrorCode.MULTI_DIM_FLATTEN_NOT_SUPPORTED);
    }

    /**
     * 递归收集所有标量值（用于统计函数）
     */
    public void collectScalars(Collection<Double> result) {
        if (isScalar()) {
            result.add(scalar);
        } else {
            for (Value v : array) {
                v.collectScalars(result);
            }
        }
    }

    @Override
    public String toString() {
        if (isScalar()) {
            return formatScalar(scalar);
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(array.get(i).toString());
        }
        sb.append("]");
        return sb.toString();
    }

    private static double normalize(double v) {
        if (Math.abs(v) < EPS) return 0.0;

        // 拉回整数
        double r = Math.rint(v); // 最近整数
        if (Math.abs(v - r) < EPS) return r;

        return v;
    }

    private static String formatScalar(double value) {
        value = normalize(value);

        if (Double.isNaN(value)) {
            return "NaN";
        }
        if (Double.isInfinite(value)) {
            return value > 0 ? "Infinity" : "-Infinity";
        }
        if (value == Math.floor(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        return DF.format(value);
    }
}

package cn.czyx007.expression_parser.ast;

import cn.czyx007.expression_parser.exception.ErrorCode;
import cn.czyx007.expression_parser.exception.ExpressionException;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;

/**
 * 值类型 - 支持标量（double）或数组（List&lt;Value&gt;）<br/>
 * 用于支持多数值变量和矩阵运算
 */
public class Value {
    private final Double scalar;
    private final List<Value> array;

    private static final double EPS = 1e-12;

    private static final DecimalFormat DF;
    static {
        // 配置数字格式化器：保留最多15位小数，四舍五入，且自动省略尾随零
        DF = new DecimalFormat("0.###############");
        DF.setRoundingMode(RoundingMode.HALF_UP);
    }

    /**
     * 构造标量值
     * @param scalar 标量值
     */
    public Value(double scalar) {
        this.scalar = scalar;
        this.array = null;
    }

    /**
     * 构造数组值
     * @param array 数组值
     */
    public Value(List<Value> array) {
        this.scalar = null;
        this.array = array;
    }

    /**
     * 判断当前值是否为标量
     * @return 如果是标量返回 true，否则返回 false
     */
    public boolean isScalar() {
        return scalar != null;
    }

    /**
     * 判断当前值是否为数组
     * @return 如果是数组返回 true，否则返回 false
     */
    public boolean isArray() {
        return array != null;
    }

    /**
     * 将当前值转换为标量
     * @return 标量值
     * @throws ExpressionException 如果当前值不是标量
     */
    public double asScalar() {
        if (!isScalar()) {
            throw new ExpressionException(ErrorCode.ARRAY_TO_SCALAR_ERROR);
        }
        return scalar;
    }

    /**
     * 将当前值转换为数组
     * @return 数组值
     * @throws ExpressionException 如果当前值不是数组
     */
    public List<Value> asArray() {
        if (!isArray()) {
            throw new ExpressionException(ErrorCode.SCALAR_TO_ARRAY_ERROR);
        }
        return array;
    }

    /**
     * 将数组展平为 double 数组（仅支持一维数组）
     * @return 展平后的 double 数组
     * @throws ExpressionException 如果数组是多维的
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
     * @param result 用于存储标量值的集合
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

    /**
     * 将值转换为字符串表示
     * @return 字符串表示
     */
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

    /**
     * 将接近零或整数的值标准化
     * @param v 待标准化的值
     * @return 标准化后的值
     */
    private static double normalize(double v) {
        if (Math.abs(v) < EPS) return 0.0;

        // 拉回整数
        double r = Math.rint(v); // 最近整数
        if (Math.abs(v - r) < EPS) return r;

        return v;
    }

    /**
     * 格式化标量值为字符串
     * @param value 标量值
     * @return 格式化后的字符串
     */
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

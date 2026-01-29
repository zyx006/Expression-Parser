package cn.czyx007.expression_parser.ast;

import java.util.HashMap;
import java.util.Map;

// 抽象语法树(AST)节点
public abstract class ExprNode {
    // 精度阈值，用于处理浮点数误差
    private static final double EPSILON = 1e-14;

    /**
     * 无参数求值（适用于不含变量的表达式）
     */
    public double eval() {
        return eval(null);
    }

    /**
     * 带上下文求值（支持变量）- 仅返回标量
     * @param context 变量名到值的映射，可为 null
     * @deprecated 建议使用 evalValue 方法以支持数组
     */
    public abstract double eval(Map<String, Double> context);

    /**
     * 带上下文求值（支持数组和标量）
     * @param context 变量名到 Value 的映射，可为 null
     * @return Value 对象（标量或数组）
     */
    public Value evalValue(Map<String, Object> context) {
        // 默认实现：将旧的 context 转换为 Double context 并调用 eval
        // 子类可以重写此方法以支持数组
        Map<String, Double> doubleContext = null;
        if (context != null) {
            doubleContext = new HashMap<>();
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                Object val = entry.getValue();
                if (val instanceof Double) {
                    doubleContext.put(entry.getKey(), (Double) val);
                } else if (val instanceof Value) {
                    Value v = (Value) val;
                    if (v.isScalar()) {
                        doubleContext.put(entry.getKey(), v.asScalar());
                    }
                }
            }
        }
        return new Value(fixPrecision(eval(doubleContext)));
    }

    /**
     * 判断此节点是否为数组表达式
     */
    public boolean isArrayExpression() {
        return false;
    }

    /**
     * 修正浮点数精度误差
     * 如果一个数非常接近整数，则四舍五入到整数
     * 否则保留足够的有效数字
     */
    protected static double fixPrecision(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return value;
        }
        // 检查是否非常接近整数
        double rounded = Math.round(value);
        if (Math.abs(value - rounded) < EPSILON) {
            return rounded;
        }
        // 检查是否非常接近某个有限精度的小数（处理如 123.00000000000001 的情况）
        // 使用 12 位有效数字进行四舍五入
        if (value != 0) {
            double scale = Math.pow(10, 12 - Math.ceil(Math.log10(Math.abs(value))));
            double scaledRounded = Math.round(value * scale) / scale;
            if (Math.abs(value - scaledRounded) < EPSILON * Math.abs(value)) {
                return scaledRounded;
            }
        }
        return value;
    }
}

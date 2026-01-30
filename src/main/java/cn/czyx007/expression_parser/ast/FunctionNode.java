package cn.czyx007.expression_parser.ast;

import cn.czyx007.expression_parser.utils.FunctionRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cn.czyx007.expression_parser.utils.FunctionRegistry.FUNCTION_REGISTRY;
import static cn.czyx007.expression_parser.utils.FunctionRegistry.MATRIX_FUNCTION_REGISTRY;

/**
 * 函数调用节点
 * - 支持标量数学函数（sin, avg, var 等）
 * - 支持矩阵函数（transpose, matmul, mean(matrix, axis) 等）
 * 设计说明：
 * 1. 普通函数通过 FUNCTION_REGISTRY 注册，参数会被展开为标量
 * 2. 矩阵函数通过 MATRIX_FUNCTION_REGISTRY 注册，参数以 Value 原结构传递
 * 3. eval() 用于纯标量表达式，evalValue() 支持数组 / 矩阵语义
 */
public class FunctionNode extends ExprNode {
    private final String funcName;
    private final List<ExprNode> args;

    // ========== 构造函数 ==========
    public FunctionNode(String funcName, List<ExprNode> args) {
        this.funcName = funcName.toLowerCase(); // 函数名不区分大小写
        this.args = args;
    }

    // ========== 求值逻辑 ==========
    @Override
    public double eval(Map<String, Double> context) {
        // 从注册表中查找函数
        FunctionRegistry.MathFunction func = FUNCTION_REGISTRY.get(funcName);
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
        FunctionRegistry.MatrixFunction matrixFunc = MATRIX_FUNCTION_REGISTRY.get(funcName);
        if (matrixFunc != null) {
            // 矩阵函数：不展开数组，保留结构
            List<Value> argValues = new ArrayList<>();
            for (ExprNode arg : args) {
                argValues.add(arg.evalValue(context));
            }
            return matrixFunc.apply(argValues);
        }

        // 普通函数：从注册表中查找
        FunctionRegistry.MathFunction func = FUNCTION_REGISTRY.get(funcName);
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

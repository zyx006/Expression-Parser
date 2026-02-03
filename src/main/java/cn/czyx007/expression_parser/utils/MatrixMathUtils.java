package cn.czyx007.expression_parser.utils;

import cn.czyx007.expression_parser.ast.Value;
import cn.czyx007.expression_parser.exception.ErrorCode;
import cn.czyx007.expression_parser.exception.ExpressionException;

import java.util.ArrayList;
import java.util.List;

/**
 * 矩阵相关运算工具类
 */
final class MatrixMathUtils {

    // ========== 公共校验方法 ==========

    /**
     * 校验值是否为向量或矩阵
     * @param value 待校验的值
     * @param funcName 函数名称（用于错误信息）
     * @return 数组的行列表
     * @throws ExpressionException 如果值不是数组或为空
     */
    private static List<Value> validateArray(Value value, String funcName) {
        if (!value.isArray()) {
            throw new ExpressionException(ErrorCode.MATRIX_REQUIRED, funcName);
        }
        List<Value> rows = value.asArray();
        if (rows.isEmpty()) {
            throw new ExpressionException(ErrorCode.MATRIX_EMPTY, funcName);
        }
        return rows;
    }

    /**
     * 校验是否为矩阵（二维数组），返回行数和列数
     * @param rows 数组的行列表
     * @param funcName 函数名称（用于错误信息）
     * @return 包含行数和列数的数组 [行数, 列数]
     * @throws ExpressionException 如果不是矩阵或行列不一致
     */
    private static int[] validateMatrix(List<Value> rows, String funcName) {
        if (!rows.get(0).isArray()) {
            throw new ExpressionException(ErrorCode.MATRIX_NOT_VECTOR, funcName);
        }
        int numRows = rows.size();
        int numCols = rows.get(0).asArray().size();

        // 验证所有行的列数相同
        for (int i = 0; i < numRows; i++) {
            Value row = rows.get(i);
            if (!row.isArray()) {
                throw new ExpressionException(ErrorCode.MATRIX_ROW_NOT_ARRAY, funcName, i + 1);
            }
            if (row.asArray().size() != numCols) {
                throw new ExpressionException(ErrorCode.MATRIX_INCONSISTENT_COLS, funcName);
            }
        }
        return new int[]{numRows, numCols};
    }

    /**
     * 校验是否为方阵
     * @param numRows 行数
     * @param numCols 列数
     * @param funcName 函数名称（用于错误信息）
     * @throws ExpressionException 如果不是方阵
     */
    private static void validateSquareMatrix(int numRows, int numCols, String funcName) {
        if (numRows != numCols) {
            throw new ExpressionException(ErrorCode.MATRIX_SQUARE_REQUIRED, funcName);
        }
    }

    /**
     * 将矩阵转换为 double[][]，同时进行元素校验
     * @param rows 数组的行列表
     * @param numRows 行数
     * @param numCols 列数
     * @param funcName 函数名称（用于错误信息）
     * @return 二维 double 数组
     * @throws ExpressionException 如果元素不是标量
     */
    private static double[][] toDoubleMatrix(List<Value> rows, int numRows, int numCols, String funcName) {
        double[][] mat = new double[numRows][numCols];
        for (int i = 0; i < numRows; i++) {
            List<Value> row = rows.get(i).asArray();
            for (int j = 0; j < numCols; j++) {
                Value elem = row.get(j);
                if (!elem.isScalar()) {
                    throw new ExpressionException(ErrorCode.MATRIX_ELEMENT_NOT_SCALAR, funcName);
                }
                mat[i][j] = elem.asScalar();
            }
        }
        return mat;
    }

    /**
     * 矩阵转置（支持向量和矩阵）
     * @param matrix 待转置的矩阵或向量
     * @return 转置后的矩阵或向量
     * @throws ExpressionException 如果输入不是有效的矩阵或向量
     */
    static Value transposeMatrix(Value matrix) {
        List<Value> rows = validateArray(matrix, "transpose");

        // 判断是向量还是矩阵
        boolean isRowVector = !rows.get(0).isArray();

        if (isRowVector) {
            // 行向量 [1,2,3] -> 列向量 [[1],[2],[3]]
            List<Value> result = new ArrayList<>();
            for (Value elem : rows) {
                List<Value> row = new ArrayList<>();
                row.add(elem);
                result.add(new Value(row));
            }
            return new Value(result);
        } else {
            // 矩阵转置
            int[] dims = validateMatrix(rows, "transpose");
            int numRows = dims[0];
            int numCols = dims[1];

            // 检查是否是列向量（n×1 矩阵），转置后应变为行向量
            if (numCols == 1) {
                // 列向量 [[1],[2],[3]] -> 行向量 [1,2,3]
                List<Value> result = new ArrayList<>();
                for (Value row : rows) {
                    result.add(row.asArray().get(0));
                }
                return new Value(result);
            }

            // 普通矩阵转置
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
    }

    /**
     * 计算矩阵乘法
     * @param a 左矩阵
     * @param b 右矩阵
     * @return 矩阵乘法结果
     * @throws ExpressionException 如果矩阵维度不匹配
     */
    static Value matMul(Value a, Value b) {
        List<Value> A = validateArray(a, "matmul");
        List<Value> B = validateArray(b, "matmul");

        int[] dimsA = validateMatrix(A, "matmul");
        int[] dimsB = validateMatrix(B, "matmul");

        int m = dimsA[0];  // A 的行数
        int n = dimsA[1];  // A 的列数
        int p = dimsB[0];  // B 的行数
        int k = dimsB[1];  // B 的列数

        // 维度检查：A(m×n) * B(p×k)，要求 n == p
        if (n != p) {
            throw new ExpressionException(ErrorCode.MATRIX_DIMENSION_MISMATCH, n, p);
        }

        // 转换为 double[][] 进行计算
        double[][] matA = toDoubleMatrix(A, m, n, "matmul");
        double[][] matB = toDoubleMatrix(B, p, k, "matmul");

        List<Value> result = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            List<Value> row = new ArrayList<>();
            for (int j = 0; j < k; j++) {
                double sum = 0;
                for (int t = 0; t < n; t++) {
                    sum += matA[i][t] * matB[t][j];
                }
                row.add(new Value(sum));
            }
            result.add(new Value(row));
        }
        return new Value(result);
    }

    /**
     * 计算矩阵迹（主对角线元素之和）
     * @param matrix 方阵
     * @return 矩阵迹
     * @throws ExpressionException 如果不是方阵
     */
    static double trace(Value matrix) {
        List<Value> rows = validateArray(matrix, "trace");
        int[] dims = validateMatrix(rows, "trace");
        validateSquareMatrix(dims[0], dims[1], "trace");

        double[][] mat = toDoubleMatrix(rows, dims[0], dims[1], "trace");

        double sum = 0;
        for (int i = 0; i < dims[0]; i++) {
            sum += mat[i][i];
        }
        return sum;
    }

    /**
     * 计算矩阵秩（使用高斯消元法）
     * @param matrix 矩阵
     * @return 矩阵的秩
     * @throws ExpressionException 如果输入不是有效的矩阵
     */
    static int matrixRank(Value matrix) {
        List<Value> rows = validateArray(matrix, "rank");
        int[] dims = validateMatrix(rows, "rank");
        int m = dims[0];
        int n = dims[1];

        // 将矩阵转换为二维数组
        double[][] a = toDoubleMatrix(rows, m, n, "rank");

        int rank = 0;
        for (int col = 0, row = 0; col < n && row < m; col++) {
            int pivot = row;
            // 在当前列中寻找绝对值最大的主元（提高数值稳定性）
            for (int i = row; i < m; i++) {
                if (Math.abs(a[i][col]) > Math.abs(a[pivot][col])) {
                    pivot = i;
                }
            }
            // 若该列全为 0，则跳过该列
            if (Math.abs(a[pivot][col]) < 1e-10) continue;

            // 将主元行交换到当前行
            double[] tmp = a[row];
            a[row] = a[pivot];
            a[pivot] = tmp;

            // 消去主元下方的元素
            for (int i = row + 1; i < m; i++) {
                double factor = a[i][col] / a[row][col];
                for (int j = col; j < n; j++) {
                    a[i][j] -= factor * a[row][j];
                }
            }
            row++; // 成功找到一个主元，占用一行
            rank++;// 秩加 1
        }
        return rank;
    }

    /**
     * 计算矩阵行/列均值
     * @param matrix 矩阵
     * @param axis 轴向：0 表示列均值（返回 1×n 行向量），1 表示行均值（返回 m×1 列向量）
     * @return 行或列的均值矩阵
     * @throws ExpressionException 如果输入不是有效的矩阵或轴向无效
     */
    static Value meanMatrix(Value matrix, int axis) {
        List<Value> rows = validateArray(matrix, "mean");
        int[] dims = validateMatrix(rows, "mean");
        int m = dims[0];
        int n = dims[1];

        double[][] mat = toDoubleMatrix(rows, m, n, "mean");

        if (axis == 0) { // 列均值 -> 1×n 行向量
            List<Value> row = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                double sum = 0;
                for (int i = 0; i < m; i++) {
                    sum += mat[i][j];
                }
                row.add(new Value(sum / m));
            }
            List<Value> result = new ArrayList<>();
            result.add(new Value(row));
            return new Value(result);
        }

        if (axis == 1) { // 行均值 -> m×1 列向量
            List<Value> result = new ArrayList<>();
            for (int i = 0; i < m; i++) {
                double sum = 0;
                for (int j = 0; j < n; j++) {
                    sum += mat[i][j];
                }
                List<Value> col = new ArrayList<>();
                col.add(new Value(sum / n)); // 每行一个单元素数组
                result.add(new Value(col));
            }
            return new Value(result);
        }

        throw new ExpressionException(ErrorCode.MATRIX_INVALID_AXIS);
    }

    /**
     * 计算行列式
     * @param matrix 方阵
     * @return 行列式的值
     * @throws ExpressionException 如果不是方阵
     */
    static double determinant(Value matrix) {
        List<Value> rows = validateArray(matrix, "det");
        int[] dims = validateMatrix(rows, "det");
        validateSquareMatrix(dims[0], dims[1], "det");

        // 转换为 double[][]
        double[][] mat = toDoubleMatrix(rows, dims[0], dims[1], "det");

        return calculateDeterminant(mat, dims[0]);
    }

    /**
     * 递归计算行列式（使用拉普拉斯展开）
     * @param mat 方阵的二维数组表示
     * @param n 方阵的维度
     * @return 行列式的值
     */
    static double calculateDeterminant(double[][] mat, int n) {
        if (n == 1) {
            return mat[0][0];
        }
        if (n == 2) {
            return mat[0][0] * mat[1][1] - mat[0][1] * mat[1][0];
        }
        if (n == 3) {
            return mat[0][0] * (mat[1][1] * mat[2][2] - mat[1][2] * mat[2][1])
                 - mat[0][1] * (mat[1][0] * mat[2][2] - mat[1][2] * mat[2][0])
                 + mat[0][2] * (mat[1][0] * mat[2][1] - mat[1][1] * mat[2][0]);
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

    /**
     * 矩阵求逆（使用高斯-约当消元法）
     * @param matrix 可逆方阵
     * @return 逆矩阵
     * @throws ExpressionException 如果矩阵不可逆（奇异矩阵）
     */
    static Value inverseMatrix(Value matrix) {
        List<Value> rows = validateArray(matrix, "inv");
        int[] dims = validateMatrix(rows, "inv");
        validateSquareMatrix(dims[0], dims[1], "inv");
        int n = dims[0];

        // 转换为 double[][] 并构建增广矩阵 [A|I]
        double[][] a = new double[n][2 * n];
        for (int i = 0; i < n; i++) {
            List<Value> row = rows.get(i).asArray();
            for (int j = 0; j < n; j++) {
                a[i][j] = row.get(j).asScalar();
                a[i][j + n] = (i == j) ? 1.0 : 0.0;  // 右侧单位矩阵
            }
        }

        // 高斯-约当消元
        for (int i = 0; i < n; i++) {
            // 找主元（绝对值最大）
            int maxRow = i;
            for (int k = i + 1; k < n; k++) {
                if (Math.abs(a[k][i]) > Math.abs(a[maxRow][i])) {
                    maxRow = k;
                }
            }

            // 交换当前行与主元行
            double[] temp = a[i];
            a[i] = a[maxRow];
            a[maxRow] = temp;

            double pivot = a[i][i];
            if (Math.abs(pivot) < 1e-10) {
                throw new ExpressionException(ErrorCode.MATRIX_SINGULAR);
            }

            // 归一化当前行
            for (int j = 0; j < 2 * n; j++) {
                a[i][j] /= pivot;
            }

            // 消去其他行的当前列
            for (int k = 0; k < n; k++) {
                if (k != i) {
                    double factor = a[k][i];
                    for (int j = 0; j < 2 * n; j++) {
                        a[k][j] -= factor * a[i][j];
                    }
                }
            }
        }

        // 提取逆矩阵（右侧 n 列）
        List<Value> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<Value> row = new ArrayList<>();
            for (int j = n; j < 2 * n; j++) {
                row.add(new Value(a[i][j]));
            }
            result.add(new Value(row));
        }
        return new Value(result);
    }

    /**
     * 解线性方程组 Ax = b
     * @param matrix 系数矩阵 A（方阵）
     * @param vector 右侧向量 b（列向量）
     * @return 解向量 x
     * @throws ExpressionException 如果矩阵不可逆或维度不匹配
     */
    static Value solveLinear(Value matrix, Value vector) {
        // 校验系数矩阵 A
        List<Value> matrixRows = validateArray(matrix, "solve");
        int[] matrixDims = validateMatrix(matrixRows, "solve");
        validateSquareMatrix(matrixDims[0], matrixDims[1], "solve");
        int n = matrixDims[0];

        // 校验右侧向量 b
        List<Value> vectorRows = validateArray(vector, "solve");
        int[] vectorDims = validateMatrix(vectorRows, "solve");

        // 检查 b 是否是列向量（n×1）
        if (vectorDims[1] != 1) {
            throw new ExpressionException(ErrorCode.SOLVE_VECTOR_FORMAT);
        }
        if (vectorDims[0] != n) {
            throw new ExpressionException(ErrorCode.SOLVE_DIMENSION_MISMATCH, vectorDims[0], n);
        }

        // 解 Ax = b，通过 x = A^(-1) * b 实现
        Value invMatrix = inverseMatrix(matrix);
        return matMul(invMatrix, vector);
    }
}

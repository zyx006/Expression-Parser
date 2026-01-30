package cn.czyx007.expression_parser.utils;

import cn.czyx007.expression_parser.ast.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * 矩阵相关运算工具
 */
final class MatrixMathUtils {

    // 辅助方法：矩阵转置
    static Value transposeMatrix(Value matrix) {
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

    // 辅助方法：计算矩阵乘法
    static Value matMul(Value a, Value b) {
        List<Value> A = a.asArray(); // 左矩阵 A（m×n）
        List<Value> B = b.asArray(); // 右矩阵 B（n×k）

        int m = A.size();                   // A 的行数
        int n = A.get(0).asArray().size();  // A 的列数（也应该是 B 的行数）
        int k = B.get(0).asArray().size();  // B 的列数

        // 维度检查：A(m×n) * B(n×k)
        if (B.size() != n) {
            throw new RuntimeException("矩阵乘法维度不匹配");
        }

        List<Value> result = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            List<Value> row = new ArrayList<>();
            for (int j = 0; j < k; j++) {
                double sum = 0;
                for (int t = 0; t < n; t++) {
                    sum += A.get(i).asArray().get(t).asScalar()
                            * B.get(t).asArray().get(j).asScalar();
                }
                row.add(new Value(sum));
            }
            result.add(new Value(row));
        }
        return new Value(result);
    }

    // 辅助方法：计算矩阵迹
    static double trace(Value matrix) {
        List<Value> rows = matrix.asArray();
        int n = rows.size();
        int m = rows.get(0).asArray().size();
        if (n != m) {
            throw new RuntimeException("trace 需要方阵");
        }
        double sum = 0;
        for (int i = 0; i < n; i++) {
            sum += rows.get(i).asArray().get(i).asScalar();
        }
        return sum;
    }

    // 辅助方法：计算矩阵秩
    static int matrixRank(Value matrix) {
        List<Value> rows = matrix.asArray();
        int m = rows.size();
        int n = rows.get(0).asArray().size();

        // 将矩阵转换为二维数组
        double[][] a = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                a[i][j] = rows.get(i).asArray().get(j).asScalar();
            }
        }

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

    // 辅助方法：计算矩阵行/列均值
    // axis=0 返回 [[a,b,c]]，axis=1 返回 [[a],[b],[c]]
    static Value meanMatrix(Value matrix, int axis) {
        List<Value> rows = matrix.asArray();
        int m = rows.size();
        int n = rows.get(0).asArray().size();

        if (axis == 0) { // 列均值 -> 1×n 行向量
            List<Value> row = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                double sum = 0;
                for (int i = 0; i < m; i++) {
                    sum += rows.get(i).asArray().get(j).asScalar();
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
                    sum += rows.get(i).asArray().get(j).asScalar();
                }
                List<Value> col = new ArrayList<>();
                col.add(new Value(sum / n)); // 每行一个单元素数组
                result.add(new Value(col));
            }
            return new Value(result);
        }

        throw new RuntimeException("mean 的 axis 只能是 0 或 1");
    }

    // 辅助方法：计算行列式
    static double determinant(Value matrix) {
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
            throw new RuntimeException("det 需要一个矩阵而不是向量");
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
                throw new RuntimeException("det 需要一个方阵（行数必须等于列数）");
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
    static double calculateDeterminant(double[][] mat, int n) {
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
}

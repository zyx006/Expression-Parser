package cn.czyx007.expression_parser.exception;

/**
 * 表达式解析器错误代码枚举
 * 提供标准化的错误消息和格式化支持
 */
public enum ErrorCode {
    // 算术错误 (A-series)
    DIVISION_BY_ZERO("A001", "Division by zero"),
    MODULO_BY_ZERO("A002", "Modulo by zero"),
    FACTORIAL_NEGATIVE("A003", "Factorial requires a non-negative integer, but got %s"),
    FACTORIAL_TOO_LARGE("A004", "Factorial parameter too large (maximum supported is 170!)"),
    INTEGER_REQUIRED("A005", "%s requires integer arguments"),
    PARAM_MUST_BE_POSITIVE("A006", "%s parameter must be greater than 0"),
    PARAM_MUST_BE_NON_NEGATIVE("A007", "%s parameter must be non-negative"),
    SQRT_NEGATIVE("A008", "sqrt parameter cannot be negative"),
    STD_DEV_ZERO("A009", "Standard deviation is 0, cannot calculate correlation coefficient"),

    // 语法错误 (S-series)
    SYNTAX_ERROR("S001", "Syntax error at position %d: expected %s, but got %s '%s'"),
    EMPTY_EXPRESSION("S002", "Expression cannot be empty"),
    UNEXPECTED_TOKEN("S003", "Syntax error at position %d: expected number, identifier, left parenthesis or left bracket, but got %s '%s'"),
    EXTRA_CONTENT("S004", "Syntax error at position %d: extra content after expression '%s'"),
    ILLEGAL_CHARACTER("S005", "Illegal character '%s' at position %d"),
    INVALID_SCIENTIFIC_NOTATION("S006", "Invalid scientific notation format at position %d: expected digit"),
    UNKNOWN_CONSTANT("S007", "Unknown constant: %s"),

    // 类型错误 (T-series)
    ARRAY_NOT_SUPPORTED_LEFT("T001", "Operator '%s' does not support array as left operand"),
    ARRAY_NOT_SUPPORTED_RIGHT("T002", "Operator '%s' does not support array as right operand"),
    ARRAY_NOT_SUPPORTED_UNARY("T003", "Unary operator '%s' does not support array operands"),
    ARRAY_NOT_SUPPORTED_FACTORIAL("T004", "Factorial operator '!' does not support array operands"),
    SCALAR_REQUIRED("T005", "%s axis parameter must be a scalar"),
    ARRAY_CANNOT_EVAL_AS_SCALAR("T006", "Array cannot be evaluated as a scalar, please use evalValue method"),
    UNDEFINED_VARIABLE("T007", "Undefined variable: %s"),
    INVALID_VARIABLE_TYPE("T008", "Variable %s has incorrect value type"),
    ARRAY_TO_SCALAR_ERROR("T009", "Cannot convert array to scalar"),
    SCALAR_TO_ARRAY_ERROR("T010", "Cannot convert scalar to array"),
    MULTI_DIM_FLATTEN_NOT_SUPPORTED("T011", "Flattening multi-dimensional arrays is not yet supported"),

    // 函数错误 (F-series)
    UNKNOWN_FUNCTION("F001", "Unknown function: %s"),
    INVALID_ARG_COUNT("F002", "Function %s requires %d argument(s), but got %d"),
    INVALID_MIN_ARG_COUNT("F003", "Function %s requires at least %d argument(s), but got %d"),
    LOG_INVALID_ARGS("F004", "Function log requires 1 or 2 arguments, but got %d"),
    PERCENTILE_RANGE("F005", "Percentile must be between 0 and 100"),
    COV_EVEN_ARGS("F006", "Covariance requires an even number of arguments (first half is X, second half is Y)"),
    COV_MIN_PAIRS("F007", "Covariance calculation requires at least %d data pairs"),
    CORR_EVEN_ARGS("F008", "Correlation requires an even number of arguments (first half is X, second half is Y)"),
    CORR_MIN_PAIRS("F009", "Correlation calculation requires at least 2 data pairs"),
    DOT_EVEN_ARGS("F010", "Dot product requires an even number of arguments (first half is vector X, second half is vector Y)"),
    DIST_EVEN_ARGS("F011", "Distance calculation requires an even number of arguments (first half is point X, second half is point Y)"),
    MANHATTAN_EVEN_ARGS("F012", "Manhattan distance requires an even number of arguments (first half is point X, second half is point Y)"),
    ALIAS_NOT_FOUND("F013", "Cannot create alias '%s' for non-existent function '%s'"),
    VARIANCE_MIN_ARGS("F014", "Sample variance requires at least 2 arguments"),
    VARIANCE_POP_MIN_ARGS("F015", "Population variance requires at least 1 argument"),
    COVARIANCE_MIN_ARGS("F016", "Sample covariance requires at least 2 data pairs"),
    COVARIANCE_POP_MIN_ARGS("F017", "Population covariance requires at least 1 data pair"),
    CORRELATION_MIN_ARGS("F018", "Correlation calculation requires at least 2 data pairs"),
    LOG_BASE_INVALID("F019", "log base must be greater than 0 and not equal to 1"),
    LOG_PARAM_INVALID("F020", "log parameter must be greater than 0"),
    GEOMEAN_POSITIVE("F021", "geomean parameters must be greater than 0"),
    VALIDATION_ERROR("F099", "%s"),  // 通用验证错误

    // 矩阵错误 (M-series)
    MATRIX_REQUIRED("M001", "%s requires a vector or matrix as parameter"),
    MATRIX_EMPTY("M002", "%s parameter cannot be empty"),
    MATRIX_NOT_VECTOR("M003", "%s requires a matrix, not a vector"),
    MATRIX_ROW_NOT_ARRAY("M004", "%s: row %d is not an array"),
    MATRIX_INCONSISTENT_COLS("M005", "%s: all rows of the matrix must have the same number of columns"),
    MATRIX_SQUARE_REQUIRED("M006", "%s requires a square matrix (number of rows must equal number of columns)"),
    MATRIX_ELEMENT_NOT_SCALAR("M007", "%s: matrix elements must be scalars"),
    MATRIX_DIMENSION_MISMATCH("M008", "matmul: matrix dimension mismatch, left matrix column count (%d) must equal right matrix row count (%d)"),
    MATRIX_SINGULAR("M009", "Matrix is not invertible (singular matrix)"),
    MATRIX_INVALID_AXIS("M010", "mean axis can only be 0 or 1"),
    SOLVE_VECTOR_FORMAT("M011", "solve: right-hand side vector b must be a column vector (e.g., [[1],[2]]), not a row vector (e.g., [1,2])"),
    SOLVE_DIMENSION_MISMATCH("M012", "solve: row count of right-hand side vector b (%d) must equal the order of coefficient matrix A (%d)"),
    MATRIX_ALIAS_NOT_FOUND("M013", "Cannot create alias '%s' for non-existent matrix function '%s'"),
    COMB_NON_NEGATIVE("M014", "C(n,k) parameters must be non-negative"),
    PERM_NON_NEGATIVE("M015", "P(n,k) parameters must be non-negative"),

    // 运算符错误 (O-series)
    UNKNOWN_OPERATOR("O001", "Unknown operator: %s"),
    UNKNOWN_UNARY_OPERATOR("O002", "Unknown unary operator: %s");

    private final String code;
    private final String messageTemplate;

    ErrorCode(String code, String messageTemplate) {
        this.code = code;
        this.messageTemplate = messageTemplate;
    }

    /**
     * 使用参数格式化错误消息
     * @param args 格式化到消息模板中的参数
     * @return 带有错误代码的格式化错误消息
     */
    public String format(Object... args) {
        String message = String.format(messageTemplate, args);
        return "[" + code + "] " + message;
    }

    /**
     * 使用参数格式化错误消息（不带错误代码前缀）
     * @param args 格式化到消息模板中的参数
     * @return 格式化的错误消息（不含错误代码）
     */
    public String formatMessage(Object... args) {
        return String.format(messageTemplate, args);
    }

    /**
     * 获取未格式化的原始消息模板
     * @return 消息模板
     */
    public String getMessage() {
        return messageTemplate;
    }

    /**
     * 获取错误代码
     * @return 错误代码字符串
     */
    public String getCode() {
        return code;
    }
}

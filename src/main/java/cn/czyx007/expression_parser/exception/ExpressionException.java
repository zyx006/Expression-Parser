package cn.czyx007.expression_parser.exception;

/**
 * 表达式解析器自定义异常 <br/>
 * 封装错误代码，提供格式化的错误消息
 */
public class ExpressionException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Object[] params;

    /**
     * 使用错误代码和可选参数创建异常
     * @param errorCode 错误代码
     * @param params 用于消息格式化的可选参数
     */
    public ExpressionException(ErrorCode errorCode, Object... params) {
        super(errorCode.format(params));
        this.errorCode = errorCode;
        this.params = params;
    }

    /**
     * 获取错误代码
     * @return 错误代码
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 获取用于消息格式化的参数
     * @return 参数数组
     */
    public Object[] getParams() {
        return params;
    }
}

package com.zsj.meetingagent.common.exception;

/**
 * 业务异常。
 * 后续登录失败、面试状态非法、RAG 低置信度拒答等可预期错误都可以复用这个异常。
 */
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}

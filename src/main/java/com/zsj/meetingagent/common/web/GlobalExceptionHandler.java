package com.zsj.meetingagent.common.web;

import com.zsj.meetingagent.common.exception.BusinessException;
import com.zsj.meetingagent.common.result.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * Controller 只关注业务意图，异常到统一响应格式的转换集中放在这里处理。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        return ApiResponse.failure(ex.code(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(this::formatFieldError)
                .orElse("请求参数校验失败，请检查输入内容");
        return ApiResponse.failure(ApiResponse.VALIDATION_ERROR_CODE, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException ex) {
        return ApiResponse.failure(ApiResponse.VALIDATION_ERROR_CODE, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleHttpMessageNotReadable() {
        return ApiResponse.failure(ApiResponse.VALIDATION_ERROR_CODE, "请求体格式错误，请确认提交的是合法 JSON");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnexpectedException(Exception ex) {
        return ApiResponse.failure(ApiResponse.SERVICE_ERROR_CODE, "服务器内部错误，请稍后重试");
    }

    private String formatFieldError(FieldError error) {
        return fieldDisplayName(error.getField()) + error.getDefaultMessage();
    }

    private String fieldDisplayName(String field) {
        return switch (field) {
            case "username" -> "用户名";
            case "password" -> "密码";
            case "realName" -> "真实姓名";
            case "phone" -> "手机号";
            case "mail" -> "邮箱";
            case "message" -> "消息内容";
            case "model" -> "模型";
            case "temperature" -> "温度";
            case "sessionId" -> "会话";
            default -> field;
        };
    }
}

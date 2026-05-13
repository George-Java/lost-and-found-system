package com.example.lostfound.common;

import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException ex) {
        return ApiResponse.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArg(MethodArgumentNotValidException ex) {
        FieldError field = ex.getBindingResult().getFieldError();
        String msg = field == null ? "Invalid request parameters" : field.getDefaultMessage();
        return ApiResponse.fail(400, msg);
    }

    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBind(BindException ex) {
        FieldError field = ex.getBindingResult().getFieldError();
        String msg = field == null ? "Invalid request parameters" : field.getDefaultMessage();
        return ApiResponse.fail(400, msg);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOther(Exception ex) {
        return ApiResponse.fail(500, "Server error: " + ex.getMessage());
    }
}

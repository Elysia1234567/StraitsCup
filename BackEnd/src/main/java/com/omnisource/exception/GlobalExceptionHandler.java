package com.omnisource.exception;

import com.omnisource.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理应用中的各种异常，返回标准的 Result 格式
 *
 * @author OmniSource
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 业务异常 ====================

    /**
     * 处理业务异常
     *
     * @param e       业务异常
     * @param request HTTP请求
     * @return 统一响应结果
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 [{}] {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    // ==================== 参数校验异常 ====================

    /**
     * 处理参数校验异常（@Valid 注解校验失败）
     *
     * @param e       参数校验异常
     * @param request HTTP请求
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败 [{}] {}: {}", request.getMethod(), request.getRequestURI(), message);
        return Result.error(CommonErrorCode.VALIDATION_ERROR.getCode(), message);
    }

    /**
     * 处理参数绑定异常
     *
     * @param e       参数绑定异常
     * @param request HTTP请求
     * @return 统一响应结果
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败 [{}] {}: {}", request.getMethod(), request.getRequestURI(), message);
        return Result.error(CommonErrorCode.VALIDATION_ERROR.getCode(), message);
    }

    /**
     * 处理约束校验异常（@Validated 注解校验失败）
     *
     * @param e       约束校验异常
     * @param request HTTP请求
     * @return 统一响应结果
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(
            ConstraintViolationException e,
            HttpServletRequest request) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String message = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("约束校验失败 [{}] {}: {}", request.getMethod(), request.getRequestURI(), message);
        return Result.error(CommonErrorCode.VALIDATION_ERROR.getCode(), message);
    }

    /**
     * 处理缺少必需参数异常
     *
     * @param e       缺少参数异常
     * @param request HTTP请求
     * @return 统一响应结果
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParameterException(
            MissingServletRequestParameterException e,
            HttpServletRequest request) {
        String message = String.format("缺少必需参数: %s", e.getParameterName());
        log.warn("缺少参数 [{}] {}: {}", request.getMethod(), request.getRequestURI(), message);
        return Result.error(CommonErrorCode.MISSING_PARAMETER.getCode(), message);
    }

    // ==================== 参数类型转换异常 ====================

    /**
     * 处理参数类型不匹配异常
     *
     * @param e       类型不匹配异常
     * @param request HTTP请求
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatchException(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request) {
        String message = String.format("参数类型错误: %s 应为 %s 类型",
                e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知");
        log.warn("参数类型错误 [{}] {}: {}", request.getMethod(), request.getRequestURI(), message);
        return Result.error(CommonErrorCode.TYPE_MISMATCH_ERROR.getCode(), message);
    }

    /**
     * 处理请求体不可读异常（JSON格式错误）
     *
     * @param e       请求体不可读异常
     * @param request HTTP请求
     * @return 统一响应结果
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleNotReadableException(
            HttpMessageNotReadableException e,
            HttpServletRequest request) {
        log.warn("请求体解析失败 [{}] {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(CommonErrorCode.BAD_REQUEST.getCode(), "请求体格式错误，请检查JSON格式");
    }

    // ==================== HTTP 请求异常 ====================

    /**
     * 处理请求方法不支持异常
     *
     * @param e       请求方法不支持异常
     * @param request HTTP请求
     * @return 统一响应结果
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request) {
        String message = String.format("请求方法 %s 不支持，支持的方法: %s",
                e.getMethod(), String.join(", ", e.getSupportedMethods()));
        log.warn("请求方法不支持 [{}] {}: {}", request.getMethod(), request.getRequestURI(), message);
        return Result.error(CommonErrorCode.METHOD_NOT_ALLOWED.getCode(), message);
    }

    /**
     * 处理媒体类型不支持异常
     *
     * @param e       媒体类型不支持异常
     * @param request HTTP请求
     * @return 统一响应结果
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<Void> handleMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e,
            HttpServletRequest request) {
        String message = String.format("不支持的内容类型: %s", e.getContentType());
        log.warn("内容类型不支持 [{}] {}: {}", request.getMethod(), request.getRequestURI(), message);
        return Result.error(CommonErrorCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 处理接口不存在异常
     *
     * @param e       接口不存在异常
     * @param request HTTP请求
     * @return 统一响应结果
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFoundException(
            NoHandlerFoundException e,
            HttpServletRequest request) {
        log.warn("接口不存在 [{}] {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(CommonErrorCode.NOT_FOUND.getCode(), "接口不存在");
    }

    // ==================== 认证授权异常 ====================

    /**
     * 处理认证异常
     *
     * @param e       认证异常
     * @param request HTTP请求
     * @return 统一响应结果
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationException(
            AuthenticationException e,
            HttpServletRequest request) {
        log.warn("认证失败 [{}] {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(CommonErrorCode.UNAUTHORIZED.getCode(), "认证失败: " + e.getMessage());
    }

    /**
     * 处理权限不足异常
     *
     * @param e       权限不足异常
     * @param request HTTP请求
     * @return 统一响应结果
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(
            AccessDeniedException e,
            HttpServletRequest request) {
        log.warn("权限不足 [{}] {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(CommonErrorCode.FORBIDDEN.getCode(), "没有权限访问该资源");
    }

    // ==================== 通用异常 ====================

    /**
     * 处理所有未捕获的异常
     *
     * @param e       异常
     * @param request HTTP请求
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 [{}] {}", request.getMethod(), request.getRequestURI(), e);
        return Result.error(CommonErrorCode.INTERNAL_ERROR.getCode(), "系统繁忙，请稍后重试");
    }
}


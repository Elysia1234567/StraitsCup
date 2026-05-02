package com.omnisource.exception;

import com.omnisource.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("Business error [{}] {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Validation failed [{}] {}: {}", request.getMethod(), request.getRequestURI(), message);
        return Result.error(CommonErrorCode.VALIDATION_ERROR.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Binding failed [{}] {}: {}", request.getMethod(), request.getRequestURI(), message);
        return Result.error(CommonErrorCode.VALIDATION_ERROR.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(
            ConstraintViolationException e,
            HttpServletRequest request) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String message = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation [{}] {}: {}", request.getMethod(), request.getRequestURI(), message);
        return Result.error(CommonErrorCode.VALIDATION_ERROR.getCode(), message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParameterException(
            MissingServletRequestParameterException e,
            HttpServletRequest request) {
        String message = "Missing required parameter: " + e.getParameterName();
        log.warn("Missing parameter [{}] {}: {}", request.getMethod(), request.getRequestURI(), message);
        return Result.error(CommonErrorCode.MISSING_PARAMETER.getCode(), message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatchException(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request) {
        String requiredType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown";
        String message = String.format("Parameter type error: %s should be %s", e.getName(), requiredType);
        log.warn("Type mismatch [{}] {}: {}", request.getMethod(), request.getRequestURI(), message);
        return Result.error(CommonErrorCode.TYPE_MISMATCH_ERROR.getCode(), message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleNotReadableException(
            HttpMessageNotReadableException e,
            HttpServletRequest request) {
        log.warn("Request body parse failed [{}] {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(CommonErrorCode.BAD_REQUEST.getCode(), "Invalid request body");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request) {
        String supportedMethods = e.getSupportedMethods() == null
                ? ""
                : String.join(", ", e.getSupportedMethods());
        String message = String.format("Request method %s is not supported. Supported methods: %s",
                e.getMethod(), supportedMethods);
        log.warn("Method not supported [{}] {}: {}", request.getMethod(), request.getRequestURI(), message);
        return Result.error(CommonErrorCode.METHOD_NOT_ALLOWED.getCode(), message);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<Void> handleMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e,
            HttpServletRequest request) {
        String message = "Unsupported content type: " + e.getContentType();
        log.warn("Media type not supported [{}] {}: {}", request.getMethod(), request.getRequestURI(), message);
        return Result.error(CommonErrorCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFoundException(
            NoHandlerFoundException e,
            HttpServletRequest request) {
        log.warn("No handler found [{}] {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(CommonErrorCode.NOT_FOUND.getCode(), "API not found");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("System error [{}] {}", request.getMethod(), request.getRequestURI(), e);
        return Result.error(CommonErrorCode.INTERNAL_ERROR.getCode(), "System busy, please try again later");
    }
}

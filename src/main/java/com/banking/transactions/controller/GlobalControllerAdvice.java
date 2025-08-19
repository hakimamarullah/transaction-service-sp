package com.banking.transactions.controller;

import com.banking.transactions.annotations.LogResponse;
import com.banking.transactions.dto.ApiResponse;
import com.banking.transactions.exception.ApiException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestControllerAdvice
@Slf4j
@LogResponse
public class GlobalControllerAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.error("[INVALID ARGUMENTS]: {}", ex.getMessage(), ex);
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        ApiResponse<Map<String, String>> response = new ApiResponse<>();
        response.setCode(400);
        response.setMessage("Invalid Arguments");
        response.setData(errors);
        return response.toResponseEntity();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> internalServerError(Exception ex) {
        log.error("[INTERNAL SERVER ERROR]: {}", ex.getMessage(), ex);
        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(500);
        response.setMessage(ex.getMessage());

        String causeClassName = Optional.ofNullable(ex.getCause())
                .map(Throwable::getClass)
                .map(Class::getCanonicalName)
                .orElse(null);
        response.setData(causeClassName);
        return response.toResponseEntity();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<String>> httpMessageNotReadableError(HttpMessageNotReadableException ex) {
        log.error("[MISSING REQUEST BODY]: {}", ex.getMessage(), ex);
        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(400);
        response.setMessage(ex.getMessage());

        String causeClassName = Optional.ofNullable(ex.getCause())
                .map(Throwable::getClass)
                .map(Class::getCanonicalName)
                .orElse(null);
        response.setData(causeClassName);
        return response.toResponseEntity();
    }


    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<String>> methodNotSupportedExHandler(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(405);
        response.setMessage(ex.getMessage());
        response.setData(req.getRequestURI());
        return response.toResponseEntity();
    }

    @ExceptionHandler({DataIntegrityViolationException.class})
    public ResponseEntity<ApiResponse<String>> dataIntegrityViolationHandler(DataIntegrityViolationException ex) {
        log.error(ex.getMessage(), ex);
        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(400);
        response.setMessage(ex.getMostSpecificCause().getLocalizedMessage());
        response.setData(ex.getMessage());

        return response.toResponseEntity();
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<String>> missingServletRequestParameterException(MissingServletRequestParameterException ex) {
        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(400);
        response.setMessage(ex.getMessage());
        response.setData(ex.getParameterName());

        return response.toResponseEntity();
    }


    @ExceptionHandler({InvalidFormatException.class, JsonParseException.class})
    public ResponseEntity<ApiResponse<String>> jsonExceptionHandler(Exception ex) {
        log.error(ex.getMessage(), ex);
        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(400);
        response.setMessage(ex.getMessage());
        response.setData(ex.getClass().getCanonicalName());

        return response.toResponseEntity();
    }

    @ExceptionHandler({ApiException.class})
    public ResponseEntity<ApiResponse<String>> apiExceptionHandler(ApiException ex) {
        log.error(ex.getMessage());
        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(ex.getHttpCode());
        response.setMessage(ex.getMessage());
        response.setData(ex.getClass().getCanonicalName());

        return response.toResponseEntity();
    }

}

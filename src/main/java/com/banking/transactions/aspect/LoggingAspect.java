package com.banking.transactions.aspect;

import com.banking.transactions.config.LoggingContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class LoggingAspect {


    @Qualifier("loggingMapper")
    private final ObjectMapper mapper;

    private final LoggingContext loggingContext;


    @Pointcut("execution(@com.banking.transactions.annotations.LogRequestResponse * *(..))")
    public void methodWithLoggedRequestResponse() {
    }


    @Pointcut("within(@com.banking.transactions.annotations.LogRequestResponse *)")
    public void classWithLogRequestResponse() {
    }

    @Pointcut("within(@com.banking.transactions.annotations.LogResponse *)")
    public void classWithLogResponse() {
    }


    @Around("methodWithLoggedRequestResponse() || classWithLogRequestResponse()")
    public Object logRequestAndResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger logger = LogManager.getLogger(joinPoint.getTarget().getClass());

        String reqId = getTraceId();
        RequestMetadata request = new RequestMetadata((ServletRequestAttributes) RequestContextHolder.getRequestAttributes());


        Object requestPayload = Optional.ofNullable(getRequestBodyParameter(joinPoint))
                .map(this::writeAsString)
                .orElse("");

        // Log Request
        logger.info("[{}] {} | {} | REQUEST: {} QUERY: {}", request.getMethod(), request.getPath(), reqId, requestPayload, request.getQueryString());

        // Proceed with the method execution and get the response
        Object response = joinPoint.proceed();
        String responseString = Optional.ofNullable(response).map(this::writeAsString).orElse("");

        // Log Response
        logger.info("[{}] {} | {} | RESPONSE: {}", request.getMethod(), request.getPath(), reqId, responseString);

        return response;
    }

    @Around("classWithLogResponse()")
    public Object logResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger logger = LogManager.getLogger(joinPoint.getTarget().getClass());

        String reqId = getTraceId();
        RequestMetadata request = new RequestMetadata((ServletRequestAttributes) RequestContextHolder.getRequestAttributes());

        // Proceed with the method execution and get the response
        Object response = joinPoint.proceed();
        String responseString = Optional.ofNullable(response).map(this::writeAsString).orElse("");

        // Log Response
        logger.info("[{}] {} | {} | RESPONSE: {}", request.getMethod(), request.getPath(), reqId, responseString);

        return response;
    }

    private String writeAsString(Object payload) {
        try {
            if (payload instanceof String) {
                return String.valueOf(payload);
            }
            if (payload instanceof ResponseEntity<?> res) {
                return mapper.writeValueAsString(res.getBody());
            }
            return mapper.writeValueAsString(payload);
        } catch (Exception e) {
            // Do Nothing
        }
        return String.valueOf(payload);
    }


    private Object getRequestBodyParameter(ProceedingJoinPoint joinPoint) {
        try {
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            Object[] args = joinPoint.getArgs();


            for (int i = 0; i < args.length; i++) {
                Annotation[] parameterAnnotations = method.getParameterAnnotations()[i];
                for (Annotation annotation : parameterAnnotations) {
                    if (annotation instanceof RequestBody) {
                        return args[i];
                    }
                }
            }
        } catch (Exception e) {
            // Do Nothing
        }
        return null;
    }

    @Getter
    static class RequestMetadata {

        private final String path;
        private final String method;
        private final String queryString;

        public RequestMetadata(ServletRequestAttributes requestAttributes) {

            HttpServletRequest request = Optional.ofNullable(requestAttributes)
                    .map(ServletRequestAttributes::getRequest).orElse(null);

            this.path = Optional.ofNullable(request)
                    .map(HttpServletRequest::getServletPath)
                    .orElse("");

            this.method = Optional.ofNullable(request)
                    .map(HttpServletRequest::getMethod)
                    .map(String::toUpperCase)
                    .orElse("");
            this.queryString = Optional.ofNullable(request)
                    .map(HttpServletRequest::getQueryString)
                    .orElse("");

        }
    }


    private String getTraceId() {
        try {
            String traceId = Span.current().getSpanContext().getTraceId();
            if (StringUtils.isBlank(traceId)) {
                return loggingContext.getReqId();
            }
            return traceId;
        } catch (Exception e) {
            return loggingContext.getReqId();
        }
    }
}


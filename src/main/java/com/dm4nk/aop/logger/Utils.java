package com.dm4nk.aop.logger;

import com.dm4nk.aop.logger.annotations.ExcludeLog;
import com.dm4nk.aop.logger.annotations.IncludeLog;
import com.dm4nk.aop.logger.annotations.LogMethod;
import com.dm4nk.aop.logger.annotations.Loggable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

import static com.dm4nk.aop.logger.Constants.ARG_FORMAT;
import static com.dm4nk.aop.logger.Constants.DELIMITER;
import static com.dm4nk.aop.logger.Constants.PATTERN_WITHOUT_RESULT;
import static com.dm4nk.aop.logger.Constants.PATTERN_WITH_RESULT;

@Slf4j
@AllArgsConstructor
class Utils {
    private final ObjectMapper objectMapper;

    void classAnnotationLog(JoinPoint joinPoint, Object result) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogMethod logMethod = method.getAnnotation(LogMethod.class);
        if (logMethod != null)
            return;

        Loggable classAnnotation = method.getDeclaringClass().getAnnotation(Loggable.class);

        if (classAnnotation.excludeMethods()) {
            IncludeLog includeLog = method.getAnnotation(IncludeLog.class);

            if (includeLog != null) {
                methodAnnotationLog(joinPoint, true, classAnnotation.level(), result);
            }
        } else {
            ExcludeLog excludeLog = method.getAnnotation(ExcludeLog.class);

            if (excludeLog == null) {
                methodAnnotationLog(joinPoint, true, classAnnotation.level(), result);
            }
        }
    }

    void methodAnnotationLog(JoinPoint joinPoint, boolean logResult, Level level, Object result) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();
        String methodName = joinPoint.getTarget().getClass().getName() + "#" + codeSignature.getName();

        String args = StreamEx
                .zip(
                        codeSignature.getParameterNames(),
                        joinPoint.getArgs(),
                        this::getParameterString
                )
                .zipWith(
                        Arrays.stream(signature.getMethod().getParameterAnnotations()).map(this::isParameterIncluded)
                )
                .filterValues(Boolean::booleanValue)
                .keys()
                .collect(Collectors.joining(DELIMITER));

        if (logResult && signature.getReturnType() != void.class) {
            this.log(methodName, args, result, level);
        } else {
            this.log(methodName, args, level);
        }
    }

    private void log(String methodName, String args, Level level) {
        log(String.format(PATTERN_WITHOUT_RESULT, methodName, args), level);
    }

    private void log(String methodName, String args, Object result, Level level) {
        try {
            result = objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            result = String.valueOf(result);
        }
        log(String.format(PATTERN_WITH_RESULT, methodName, args, result), level);
    }

    private String getParameterString(String argumentName, Object argumentValue) {
        try {
            argumentValue = objectMapper.writeValueAsString(argumentValue);
        } catch (JsonProcessingException e) {
            argumentValue = String.valueOf(argumentValue);
        }
        return String.format(ARG_FORMAT, argumentName, argumentValue);
    }

    private boolean isParameterIncluded(Annotation[] annotations) {
        return Arrays.stream(annotations).noneMatch(annotation -> annotation instanceof ExcludeLog);
    }

    private static void log(String message, Level level) {
        switch (level) {
            case INFO -> log.info(message);
            case DEBUG -> log.debug(message);
            case TRACE -> log.trace(message);
        }
    }
}

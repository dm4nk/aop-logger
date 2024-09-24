package com.dm4nk.aop.logger;

import com.dm4nk.aop.logger.annotations.ExcludeLog;
import com.dm4nk.aop.logger.annotations.IncludeLog;
import com.dm4nk.aop.logger.annotations.LogMethod;
import com.dm4nk.aop.logger.annotations.Loggable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import one.util.streamex.StreamEx;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.dm4nk.aop.logger.Constants.ARG_FORMAT;
import static com.dm4nk.aop.logger.Constants.DELIMITER;
import static com.dm4nk.aop.logger.Constants.NO_SUCH_LEVEL_PATTERN;
import static com.dm4nk.aop.logger.Constants.WITHOUT_RESULT_PATTERN;
import static com.dm4nk.aop.logger.Constants.WITH_RESULT_PATTERN;

@AllArgsConstructor
class Utils {

    private final ObjectMapper objectMapper;

    private static final ImmutableMap<Level, BiConsumer<Logger, String>> LEVEL_TO_LOG = ImmutableMap.of(
            Level.INFO, Logger::info,
            Level.DEBUG, Logger::debug,
            Level.TRACE, Logger::trace
    );

    private static final ImmutableMap<Level, Function<Logger, Boolean>> LEVEL_TO_LOGGING_ENABLED = ImmutableMap.of(
            Level.INFO, Logger::isInfoEnabled,
            Level.DEBUG, Logger::isDebugEnabled,
            Level.TRACE, Logger::isTraceEnabled
    );

    void classAnnotationLog(JoinPoint joinPoint, Object result) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        if (method.getAnnotation(LogMethod.class) != null) {
            return;
        }

        Loggable classAnnotation = method.getDeclaringClass().getAnnotation(Loggable.class);

        if (classAnnotation.excludeMethods()) {
            if (method.getAnnotation(IncludeLog.class) != null) {
                methodAnnotationLog(joinPoint, true, classAnnotation.level(), result);
            }
        } else {
            if (method.getAnnotation(ExcludeLog.class) == null) {
                methodAnnotationLog(joinPoint, true, classAnnotation.level(), result);
            }
        }
    }

    void methodAnnotationLog(JoinPoint joinPoint, boolean logResult, Level level, Object result) {
        Class<?> targetClass = joinPoint.getTarget().getClass();
        Logger logger = LoggerFactory.getLogger(targetClass);
        if (!isLoggingEnabled(level, logger)) {
            return;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getName();

        String args = StreamEx
                .of(joinPoint.getArgs())
                .map(String::valueOf)
                .zipWith(
                        Arrays.stream(signature.getMethod().getParameterAnnotations()).map(Utils::isParameterIncluded)
                )
                .filterValues(Boolean::booleanValue)
                .keys()
                .collect(Collectors.joining(DELIMITER));

        String message = calculateMessage(
                methodName,
                args,
                result,
                logResult && signature.getReturnType() != void.class
        );

        log(message, level, logger);
    }

    private String getParameterString(String argumentName, Object argumentValue) {
        try {
            argumentValue = objectMapper.writeValueAsString(argumentValue);
        } catch (JsonProcessingException e) {
            argumentValue = String.valueOf(argumentValue);
        }
        return String.format(ARG_FORMAT, argumentName, argumentValue);
    }

    private String calculateMessage(String methodName, String args, Object result, boolean withResult) {
        if (withResult) {
            String resultString;
            try {
                resultString = objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException e) {
                resultString = String.valueOf(result);
            }
            return String.format(WITH_RESULT_PATTERN, resultString, args, resultString);
        } else {
            return String.format(WITHOUT_RESULT_PATTERN, methodName, args);
        }
    }

    private static boolean isParameterIncluded(Annotation[] annotations) {
        return Arrays.stream(annotations).noneMatch(annotation -> annotation instanceof ExcludeLog);
    }

    private static void log(String message, Level level, Logger logger) {
        Optional.ofNullable(LEVEL_TO_LOG.get(level))
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(NO_SUCH_LEVEL_PATTERN, level, LEVEL_TO_LOG.keySet()))
                )
                .accept(logger, message);
    }

    private static boolean isLoggingEnabled(Level level, Logger logger) {
        return Optional.ofNullable(LEVEL_TO_LOGGING_ENABLED.get(level))
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(NO_SUCH_LEVEL_PATTERN, level, LEVEL_TO_LOGGING_ENABLED.keySet()))
                )
                .apply(logger);
    }
}

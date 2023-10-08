package com.dm4nk.aop.logger;


import com.dm4nk.aop.logger.annotations.LogMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;


@Slf4j
@Aspect
@Component
@NoArgsConstructor
class LoggingAspect {

    private static final Utils utils = new Utils(new ObjectMapper());

    @Pointcut(value = "@annotation(methodAnnotation)", argNames = "methodAnnotation")
    private void methodAnnotation(final LogMethod methodAnnotation) {
    }

    @AfterReturning(value = "methodAnnotation(methodAnnotation))", argNames = "joinPoint, methodAnnotation, result", returning = "result")
    private void logMethod(JoinPoint joinPoint, LogMethod methodAnnotation, Object result) {
        utils.methodAnnotationLog(joinPoint, methodAnnotation.logResult(), methodAnnotation.level(), result);
    }

    @Pointcut("within(@com.dm4nk.aop.logger.annotations.Loggable *)")
    private void classAnnotation() {
    }

    @AfterReturning(value = "classAnnotation()", argNames = "joinPoint, result", returning = "result")
    private void logClass(JoinPoint joinPoint, Object result) {
        utils.classAnnotationLog(joinPoint, result);
    }
}

package com.dm4nk.aop.logger.annotations;


import com.dm4nk.aop.logger.Level;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface LogMethod {
    boolean logResult() default true;

    Level level() default Level.DEBUG;
}

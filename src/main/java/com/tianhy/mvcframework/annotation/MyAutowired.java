package com.tianhy.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @Desc:
 * @Author: thy
 * @CreateTime: 2019/3/27
 **/
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyAutowired {
    String value() default "";
}

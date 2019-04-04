package com.tianhy.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @Desc:
 * @Author: thy
 * @CreateTime: 2019/3/27
 **/
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyController {
    String value() default "";
}

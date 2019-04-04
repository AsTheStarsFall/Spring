package com.tianhy.demo.service.impl;

import com.tianhy.demo.service.TestService;
import com.tianhy.mvcframework.annotation.MyService;

/**
 * @Desc:
 * @Author: thy
 * @CreateTime: 2019/3/27
 **/
@MyService
public class TestServiceImpl implements TestService {

    @Override
    public String getName(String name) {
        return name;
    }
}

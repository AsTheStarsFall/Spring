package com.tianhy.demo.controller;

import com.tianhy.demo.service.TestService;
import com.tianhy.mvcframework.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @Desc: 控制层
 * @Author: thy
 * @CreateTime: 2019/3/27
 **/
@MyController
@MyRequestMapping("/tianhy")
public class TestController {

    @MyAutowired
    private TestService service;

    @MyRequestMapping("/query")
    public void query(HttpServletRequest req, HttpServletResponse resp, @MyRequestParam("name") String name) {
        String result = name;
        String sname= service.getName(result);
        try {
            resp.getWriter().write(sname);
        } catch (IOException e) {
            e.printStackTrace();
        }
       // System.out.println(service.getName(name));
    }

    @MyRequestMapping("/add")
    public void add(HttpServletRequest req, HttpServletResponse resp,
                    @MyRequestParam("a") Integer a, @MyRequestParam("b") Integer b) {
        try {
            resp.getWriter().write(a + "+" + b + "=" + (a + b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @MyRequestMapping("/sub")
    public void add(HttpServletRequest req, HttpServletResponse resp,
                    @MyRequestParam("a") Double a, @MyRequestParam("b") Double b) {
        try {
            resp.getWriter().write(a + "-" + b + "=" + (a - b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

package com.tianhy.mvcframework.servletversion2;

import com.tianhy.mvcframework.annotation.*;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.nutz.json.Json;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Desc: 入口
 * @Author: thy
 * @CreateTime: 2019/3/27
 **/
public class DispatcherServlet extends HttpServlet {
    public DispatcherServlet() {
    }

    /**
     * 保存application.properties配置文件的内容
     */
    private Properties contextConfig = new Properties();

    /**
     * 扫描到的所有类名
     */
    private List<String> classNames = new ArrayList<>();

    /**
     * IOC容器
     */
    private Map<String, Object> ioc = new HashMap<>();

    /**
     * URL 与 method 对应关系
     */
    // private Map<String, Method> handlerMapping = new HashMap<>();

    private List<Handler> handlerMapping = new ArrayList<Handler>();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500, ErrInfo :" + Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1、加载配置文件
        doLoadProperties(config.getInitParameter("contextConfigLocation"));
        //2、扫描相关类
        doScannerClass(contextConfig.getProperty("scanPackage"));
        //3、初始化（实例化）扫描到的类，放入IOC
        doInstance();
        //4、DI 依赖注入（自动依赖注入）
        doAutowired();
        //5、初始化HandlerMappping（URL与method的映射）
        initHandlerMapping();

    }

    //6、运行
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {

        Handler handler = getHandler(req);
        if (handler == null) {
            resp.getWriter().write("404 Not Found!");
            return;
        }

        //拿到方法的形参列表
        Class<?>[] methodParameterTypes = handler.getParamTypes();

        //存放形参的值
        Object[] methodParamtersValues = new Object[methodParameterTypes.length];

        //拿到req请求的参数，一个key对应多个value，所以是个数组
        Map<String, String[]> reqParameterMap = req.getParameterMap();
        //遍历req请求的参数, 给methodParamtersValues赋值
        for (Map.Entry<String, String[]> entry : reqParameterMap.entrySet()) {
            //参数是字符串的形式，因为是个数组所以要把[] 去掉
            //‘\\[|\\]’: '[' 或者']' 都要被替换掉
            String s = Arrays.toString(entry.getValue()).replaceAll("\\[|\\]", "");
            if (!handler.paramIndexMapping.containsKey(entry.getKey())) {
                continue;
            }
            //entry.getKey() = name
            //参数下标
            int index = handler.paramIndexMapping.get(entry.getKey());
            //参数赋值
            methodParamtersValues[index] = convert(methodParameterTypes[index], s);
        }
        if (handler.paramIndexMapping.containsKey(HttpServletRequest.class.getName())) {
            int reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
            methodParamtersValues[reqIndex] = req;
        }
        if (handler.paramIndexMapping.containsKey(HttpServletResponse.class.getName())) {
            int respIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
            methodParamtersValues[respIndex] = resp;
        }
        //执行方法
        Object invoke = handler.method.invoke(handler.controller, methodParamtersValues);
        if (invoke == null || invoke instanceof Void) {
            return;
        }
        resp.getWriter().write(invoke.toString());

    }

    /**
    * @Description: 处理路径，正则
    * @Param: [req]
    * @return: com.tianhy.mvcframework.servletversion2.DispatcherServlet.Handler
    * @Author: thy
    * @throws:
    * @Date: 2019/3/31
    */
    private Handler getHandler(HttpServletRequest req) {
        if (handlerMapping == null) {
            return null;
        }
        String url = req.getRequestURI();
        String contextUrl = req.getContextPath();
        //  ‘/+’：多个 ‘/’
        url = url.replaceAll(contextUrl, "").replaceAll("/+", "/");

        for (Handler handler : this.handlerMapping) {
            Matcher matcher = handler.getPattern().matcher(url);
            if (!matcher.matches()) {
                continue;
            }
            return handler;
        }
        return null;
    }


    /**
     * 初始化，url,method 一一对应的关系
     */
    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(MyController.class)) {
                continue;
            }

            //@MyRequestMapping("/xxx")
            String baseUrl = "";
            if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);
                // thy
                baseUrl = requestMapping.value();
            }

            //获取所有public方法
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                    continue;
                }
                MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);


                //优化,避免输入多个 / 或少输入 / 而找不到路径
                //通过获取到注解的值，与方法上@requestMapping的值拼接，生成一个路径
                String url = ("/" + baseUrl + "/" + requestMapping.value()).
                        replaceAll("/+", "/");
                Pattern compile = Pattern.compile(url);
                this.handlerMapping.add(new Handler(compile, entry.getValue(), method));
                System.out.println("mapped: " + url + " : " + method);
            }
        }
    }

    /**
     * 自动依赖注入
     */
    private void doAutowired() {
        if (ioc.isEmpty()) {
            return;
        }
        //遍历IOC容器
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //拿到所有的字段
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field f : fields) {
//               Annotation [] annotations = f.getAnnotations();
                if (!f.isAnnotationPresent(MyAutowired.class)) {
                    continue;
                }
                MyAutowired autowired = f.getAnnotation(MyAutowired.class);
                String beanName = autowired.value().trim();
                //如果没有自定义beanName，根据类型注入
                if (StringUtils.isBlank(beanName)) {
                    beanName = f.getType().getName();
                }
                f.setAccessible(true);
                try {
                    //给字段赋值
                    f.set(entry.getValue(), ioc.get(beanName));
//                    System.out.println(f.get(entry.getValue()));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    /**
     * 初始化扫描到的类,为DI作准备
     */
    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        try {
            //遍历类名
            for (String className : classNames) {
                //根据类名返回class对象
                Class<?> clazz = Class.forName(className);

                //1.什么样的类要被初始化？
                //2.加了注解的类怎么判断？

                //如果是加了@Mycontroller注解的类
                if (clazz.isAnnotationPresent(MyController.class)) {
                    //实例化
                    Object instance = clazz.newInstance();
                    //Spring默认类名首字母小写
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    //一个类名对应一个实例,保存到IOC
                    ioc.put(beanName, instance);

                    //如果是加了@MyService注解的类
                } else if (clazz.isAnnotationPresent(MyService.class)) {

                    //自定义注解的情况，比如@MyService("xxx")
                    MyService myService = clazz.getAnnotation(MyService.class);
                    String beanName = myService.value();
                    if (StringUtils.isBlank(beanName.trim())) {
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    //保存到IOC
                    ioc.put(beanName, instance);
                    //如果实现了多个接口，根据类名自动赋值
                    for (Class<?> i : clazz.getInterfaces()) {
                        if (ioc.containsKey(i.getName())) {
                            throw new Exception("The" + i.getName() + "is exist!");
                        }
                        //把类名当作Key
                        ioc.put(i.getName(), instance);
                    }
                } else {
                    continue;
                }
            }

            System.out.println(" ioc : "+ ioc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        // 之所以加，是因为大小写字母的ASCII码相差32，
        // 而且大写字母的ASCII码要小于小写字母的ASCII码
        // 在Java中，对char做算学运算，实际上就是对ASCII码做算学运算
        chars[0] += 32;
        return String.valueOf(chars);
    }

    /**
     * 扫描类
     *
     * @param scanPackage
     */
    private void doScannerClass(String scanPackage) {
        //由包路径com.tianhy.controller 转换为文件路径,就是把 . 替换成 /
        // \\: 转义的意义在于，java中的路径为字符串，无法识别是否为路径
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classPath = new File(url.getFile());
        //F:\StudyWorkSpaces\Spring-v1\target\classes
        // 遍历当前路径下的所有文件
        for (File file : classPath.listFiles()) {
            //如果是根目录，递归
            if (file.isDirectory()) {
                doScannerClass(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    continue;
                }
                String className = scanPackage + "." + file.getName().replace(".class", "");
                //将扫描到的类名保存
                classNames.add(className);
            }
        }

    }

    /**
     * 加载配置文件
     *
     * @param contextConfigLocation
     */
    private void doLoadProperties(String contextConfigLocation) {
        //从类路径下找到application.properties文件
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            //保存到Properties对象中
            //相当于scanPackage=com.tianhy.controller从文件写到内存中
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //从url获取到的参数都是string类型,http基于字符串
    //类型转换
    public Object convert(Class<?> type, String value) {
        if(String.class == type){
            return value;
        }
        if (Integer.class == type) {
            return Integer.valueOf(value);
        } else if (Double.class == type) {
            return Double.valueOf(value);
        }

        return value;
    }

    /**
     * 新建内部类，重新设计URL 与 method映射的逻辑
     */
    @Getter
    public class Handler {
        private Method method;
        private Object controller;
        private Pattern pattern;
        //参数列表
        private Class<?>[] paramTypes;

        /**
         * 将参数与参数的位置保存
         */
        private Map<String, Integer> paramIndexMapping;

        public Handler(Pattern pattern, Object controller, Method method) {
            this.pattern = pattern;
            this.method = method;
            this.controller = controller;
            this.paramTypes = method.getParameterTypes();
            this.paramIndexMapping = new HashMap<>();
            putParamIndexMapping(method);
        }

        /**
         * 将方法中的参数与其位置关联
         */
        private void putParamIndexMapping(Method method) {

            /**
             * 方法上加了注解的参数
             */
            //拿到方法上的注解
            Annotation[][] anno = method.getParameterAnnotations();
            for (int i = 0; i < anno.length; i++) {
                //解析注解@MyRequestParam
                for (Annotation a : anno[i]) {
                    if (a instanceof MyRequestParam) {
                        //获取注解的value
                        String value = ((MyRequestParam) a).value();
                        if (StringUtils.isNotBlank(value.trim())) {
                            //参数与位置保存
                            paramIndexMapping.put(value, i);
                        }
                    }
                }
            }


            /**
             * req与resp参数
             */
            //拿到方法的形参列表
            Class<?>[] methodParameterTypes = method.getParameterTypes();
            //遍历方法的形参列表
            for (int i = 0; i < methodParameterTypes.length; i++) {
                //每一个方法的形参
                Class<?> methodParamType = methodParameterTypes[i];
                //判断是否为req/resp类型，拿到参数名称，分别放入paramIndexMapping
                if ((methodParamType == HttpServletRequest.class) || methodParamType == HttpServletResponse.class) {
                    paramIndexMapping.put(methodParamType.getName(), i);
                }
            }
        }
    }
}

### SpringIOC(Inversion Of Control)控制反转
在Spring初始化阶段，通过配置文件，将扫描到的类和加了注解的类，通过反射实例化，并且注入IOC容器，让IOC容器来管理。就是说通过配置文件或
注解的方式来管理对象之间的依赖关系。依赖倒置思想，高层决定做什么，底层去实现。
### DI(Dependency Injection)依赖注入
给@Autowired的字段赋值
### MVC
DispatcherServlet
HandlerMapping
HandlerAdapter
Controller
ModeAndView
ViewResolver
View
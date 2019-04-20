### SpringIOC(Inversion Of Control)控制反转
在Spring初始化阶段，通过配置文件，将扫描到的类和加了注解的类，通过反射实例化，并且注入IOC容器，让IOC容器来管理。就是说通过配置文件或
注解的方式来管理对象之间的依赖关系。

定位，加载，注册
***
### DI(Dependency Injection)依赖注入
IOC通过DI来实现

初始化Bean并且将它注入IOC容器当中
***
### MVC
DispatcherServlet
* HandlerMapping
* HandlerAdapter
* ModeAndView
* ViewResolver
* View
(S)[https://www.processon.com/diagraming/5cbaa884e4b06bcc138439ce]

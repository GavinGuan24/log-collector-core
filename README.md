# log-collector-core

log-collector-core is a implementation of log collector for spring and logback framework.

该项目已视为我巩固java网络编程的学习项目, 性能不佳, 已停止维护.

### 简介
**log-collector-core** 是一个小型日志收集框架, 基于TCP链接, 应用在**spring**与**logback**中, 实现了 **LogReceiver** 与 **GGTcpSocketAppender** 这两个类.

**注**: **log-collector-core** 并不是完整解决方案, 只是[**log-collector**](https://github.com/GavinGuan24/log-collector/)的核心逻辑, 完整解决方案请关注我的个人开源项目[**log-collector**](https://github.com/GavinGuan24/log-collector/), 
但[**log-collector**](https://github.com/GavinGuan24/log-collector/)现在还是预览版, 应为没有编写静态文件与实现的接口.
我将会在[**log-collector**](https://github.com/GavinGuan24/log-collector/)整合我的[**hawkeye**](https://github.com/GavinGuan24/hawkeye).

-  **LogReceiver** 负责接收日志
-  **GGTcpSocketAppender** 负责发送日志, 支持自动重连

话不多说, 直接说如何使用


### 1. LogReceiver
#### 1.1 Spring Boot 中使用

##### 1.1.1 **LogReceiver** 注入 **Spring** 环境

```java
@Configuration
public class ComponentConfig {

    @Bean(name = "LogReceiver")
    public LogReceiver getLogReceiver() {
        return new LogReceiver();
        //LogReceiver(int connectionMax)
        //LogReceiver(int port, int connectionMax)
        //默认 5544 为接收日志的端口, 处理器核心数*16 为最大链接数
    }

}
```

##### 1.1.2 **Spring Boot** 初始化组件之后, 初始化 **LogReceiver**

如下是 **Spring** 组件初始化之后调用 **ApplicationListener** 的代码示例

```java
public class ApplicationReadyEventListener implements ApplicationListener<ApplicationReadyEvent> {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        LogReceiver logReceiver = applicationReadyEvent.getApplicationContext().getBean(LogReceiver.class);
        boolean error = false;
        try {
            logReceiver.startupListener();
        } catch (Exception e) {
            e.printStackTrace();
            error = true;
        }
        if (error) logger.error("LogReceiver.init failed");
    }
}
```

##### 1.1.3 将 **ApplicationListener** 注入 **Spring** 的环境


```java

public class App {
    public static void main(String[] args) {
    		SpringApplication app = new SpringApplication(LogcollectorApplication.class);
    		app.addListeners(new ApplicationReadyEventListener());
    		app.run(args);
    }
}

```

#### 1.2 spring mvc 中使用
和 1.1 spring boot 中使用基本相似, 只不过老框架需要配置在 **```spring*.xml```** 文件中, 在此不做过多解释, 老鸟应该自己知道怎么搞


###### 至此, 日志的接收功能已经完成(暂时未编写自定义配置文件, 后续版本中会加入), 接下来是日志的发送

### 2. GGTcpSocketAppender
使用过**logback**的都知道有个配置文件 **```logback.xml```** 或者 **```logback-spring.xml```**,
命名不同, 会有一些不同之处, 在此不做赘述
主要说明一下 **logback** 配置文件中, 加入**GGTcpSocketAppender** 
#### 2.1 configuration 节点下, 增加一个 appender 节点

```xml
<!--host当然就是收集该值日的服务器ip, port当然就是定义好的端口, timeoutSecond是对方无应答的超时时间-->
<appender name="GG" class="org.gavin.log.collector.service.log.sender.GGTcpSocketAppender">
    <host>${logCollectorHost}</host>
    <port>${logCollectorPort}</port>
    <timeoutSecond>${logCollectorTimeoutSecond}</timeoutSecond>
</appender>

```
#### 2.2 root 节点下加一个对刚才appender的引用
```xml
<root level="${logLevel}">
    <appender-ref ref="console" /> <!--这两个就是最常用的控制台输出与日志文件输出, 如果不需要, 你也可以不配置-->
    <appender-ref ref="file" /> 
    
    <appender-ref ref="GG" /> <!--这里就是对 GGTcpSocketAppender 的引用-->
</root>
```


至此, 即是对 **log-collector-core** 的使用描述, 可能不够详细, 我的 **log-collector** 预览版已公开, 你可以看到源码中对 **log-collector-core** 的使用




待续...




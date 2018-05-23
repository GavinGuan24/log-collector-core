#log-collector-core

###简介
**log-collector-core** 是一个小型日志收集框架, 基于TCP链接, 实现了 **LogReceiver** 与 **GGTcpSocketAppender** 这两个类.
###注: **log-collector-core** 并不是完整解决方案, 没有实现对接收到的日志数据的 分类、持久化、查询

-  **LogReceiver** 负责接收日志
-  **GGTcpSocketAppender** 负责发送日志, 支持自动重连

话不多说, 直接说如何使用


### 1. LogReceiver
#### 1.1 Spring Boot 中使用

#####1.1.1 **LogReceiver** 注入 **Spring** 环境

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

至此, 日志的接收功能已经完成




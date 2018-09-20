# oxygen

轻量级框架


## 介绍

一个轻量级框架，包含ioc、aop、配置管理、密码加密、异常处理等

## 特性

* ioc容器
* aop切面
* config配置

## 安装

添加依赖到你的 pom.xml:
```
<dependency>
    <groupId>vip.justlive</groupId>
    <artifactId>oxygen-core</artifactId>
    <version>${oxygen.version}</version>
</dependency>
```

## 快速开始

### 基础返回

使用 `Resp` 作为返回

```
// 成功返回 code 00000
Resp.success(Object obj);

// 错误返回 默认code 99999
Resp.error(String msg);

// 错误返回 自定义code
Resp.error(String code, String msg);
```

### 异常处理

使用 `Exceptions` 抛出异常

```
// 创建 ErrorCode
ErrorCode err = Exceptions.errorCode(String module, String code);
ErrorCode err = Exceptions.errorMessage(String module, String code, String message);

// 抛出unchecked异常
throw Exceptions.wrap(Throwable e);
throw Exceptions.wrap(Throwable e, String code, String message);
throw Exceptions.wrap(Throwable e, ErrorCode errorCode, Object... arguments);

// 抛出业务异常 不含堆栈信息
throw Exceptions.fail(ErrorCode errCode, Object... params);
throw Exceptions.fail(String code, String message, Object... params);

// 抛出故障异常 包含堆栈信息
throw Exceptions.fault(ErrorCode errCode, Object... params);
throw Exceptions.fault(String code, String message, Object... params);
throw Exceptions.fault(Throwable e, ErrorCode errCode, Object... params);
throw Exceptions.fault(Throwable e, String code, String message, Object... params)

```

### IOC 

通过注解使用IOC容器

```
// 使用 @Configuration 和 @Bean
@Configuration
public class Conf {
 
  @Bean
  Inter noDepBean() {
    return new NoDepBean();
  }
}

// 使用 @Bean 和 @Inject
@Bean("depBean")
public class DepBean implements Inter {

  private final NoDepBean noDepBean;

  @Inject
  public DepBean(NoDepBean noDepBean) {
    this.noDepBean = noDepBean;
  }
  
  ...
}

// 运行时获取bean
Inter inter = BeanStore.getBean("depBean", Inter.class);

```

### AOP

通过注解使用AOP

```
// 定义使用了Log注解的方法aop处理
@Before(annotation = Log.class)
public void log(Invocation invocation) {
  ...
}

// 目标方法添加注解
@Log
public void print() {
  ...
}  
```

### 定时任务

使用注解 `@Scheduled` 标记一个方法需要作为定时任务

onApplicationStart(), cron(), fixedDelay(), or fixedRate() 必须配置其中一个

```
// 固定延迟任务 任务结束时间-下一个开始时间间隔固定
@Scheduled(fixedDelay = "500")
public void run1() {
  ...
}

// 固定周期任务 任务开始时间-下一个开始时间固定
@Scheduled(fixedRate = "600")
public void run2() {
  ...
}

// cron任务，并且程序启动后异步执行一次
@Scheduled(cron = "0/5 * * * * ?", onApplicationStart = true, async = true)
public void run3() {
  ...
}
```

## 联系信息

E-mail: qq11419041@163.com

QQ: 1106088328
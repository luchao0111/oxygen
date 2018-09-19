# oxygen

light framework


## Documentation

a light framework include ioc, aop, config manager, crypto encoder, exceptions and so on.

## Features

* ioc, bean manager
* aop, aspect
* config, properties manager

## Install

Add dependencies to your pom.xml:
```
<dependency>
    <groupId>vip.justlive</groupId>
    <artifactId>oxygen-core</artifactId>
    <version>${oxygen.version}</version>
</dependency>
```

## Quick start

### Common Response

we use `Resp` as response

```
// success response with code 00000
Resp.success(Object obj);

// error response with default code 99999
Resp.error(String msg);

// error response with custom code
Resp.error(String code, String msg);
```

### Exceptions

we use `Exceptions` to throw a runtime exception

```
// create instance of ErrorCode
ErrorCode err = Exceptions.errorCode(String module, String code);
ErrorCode err = Exceptions.errorMessage(String module, String code, String message);

// throw an unchecked exception wrapped
throw Exceptions.wrap(Throwable e);
throw Exceptions.wrap(Throwable e, String code, String message);
throw Exceptions.wrap(Throwable e, ErrorCode errorCode, Object... arguments);

// throw a business exception with none stack trace
throw Exceptions.fail(ErrorCode errCode, Object... params);
throw Exceptions.fail(String code, String message, Object... params);

// throw a fault with stack trace
throw Exceptions.fault(ErrorCode errCode, Object... params);
throw Exceptions.fault(String code, String message, Object... params);
throw Exceptions.fault(Throwable e, ErrorCode errCode, Object... params);
throw Exceptions.fault(Throwable e, String code, String message, Object... params)

```

### IOC 

you can use IOC container with annotation

```
// use @Configuration and @Bean
@Configuration
public class Conf {
 
  @Bean
  Inter noDepBean() {
    return new NoDepBean();
  }
}

// use @Bean and @Inject
@Bean("depBean")
public class DepBean implements Inter {

  private final NoDepBean noDepBean;

  @Inject
  public DepBean(NoDepBean noDepBean) {
    this.noDepBean = noDepBean;
  }
  
  ...
}

// get bean at runtime
Inter inter = BeanStore.getBean("depBean", Inter.class);

```

### AOP

we can use aop with annotations

```
// define aop method
@Before(annotation = Log.class)
public void log(Invocation invocation) {
  ...
}

// target method
@Log
public void print() {
  ...
}  
```

## Contact information

E-mail: qq11419041@163.com

QQ: 1106088328
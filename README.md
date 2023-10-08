# aop-logger

[AOP](https://en.wikipedia.org/wiki/Aspect-oriented_programming) logger for [Spring Boot](https://spring.io/projects/spring-boot) applications.

## Basic usage

Annotate method with `@LogMethod` to log name, input arguments and result of method invocation, or `@Loggable` for all methods of the component.

```java
@Service
public class Service {

    @LogMethod
    public TestObject test(String name1, String name2, Integer integer) {
        TestObject testObject = new TestObject(
                1L,
                name1,
                new InnerTestObject(
                        2L,
                        name2
                )
        );

        return testObject;
    }
}
```

Log message:

> com.dm4nk.aoptest.Service#test(name1="Outer", name2="Inner", integer=2): {"id":1,"name":"AOP","inner":{"id":2,"name":"none"}}

---
- `@LogMethod(logResult = false)` skips result logging
- `@LogMethod(level = Level.INFO)` changes level of aop-logger messages. `Level.DEBUG` is set by default
- `@Loggable(excludeMethods = true)` skips logging of all methods not annotated with `@IncludeLog`
- `@ExcludeLog` excludes method or method parameter from logging message

## Include to your project

We will use [jitpack.io](https://jitpack.io/) project to get github repository as maven library.

Add to `pom.xml`
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
  <dependency>
      <groupId>com.github.dm4nk</groupId>
      <artifactId>aop-logger</artifactId>
      <version>1.0</version>
  </dependency>
</dependencies>
```

Add `"com.dm4nk.aop.logger"` to pakage scan.

```java
@SpringBootApplication(
        scanBasePackages = {
                "com.dm4nk.aop.logger",
                "your.project.package"
        }
)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```




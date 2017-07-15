# springcloud LCN分布式事务示例demo


## 使用说明

框架分为jdbc／jpa／mybatis三个版本。各个版本之间除了DB框架差异以外，其他配置都相同。

demo分为两类，demo1/demo2 只是消费者与提供者两个的简单demo。以及demo1/2/3/4/5复杂类型调用关系。

demo1/demo2类型：

demo1作为消费者（分布式事务的发起者）调用demo2.

demo1/2/3/4/5类型：

demo1作为分布式事务的发起者，调用了demo2 demo3，demo3有调用了demo4 demo5.

## 使用步骤

1. 启动[TxManager](https://github.com/1991wangliang/tx-manager-war) 

2. 添加配置maven库与tx-lcn库

maven私有仓库地址：
```
	<repositories>
		<repository>
			<id>lorne</id>
			<url>https://1991wangliang.github.io/repository</url>
		</repository>
	</repositories>

```
maven jar地址 

```
		<dependency>
			<groupId>com.lorne.tx</groupId>
			<artifactId>springcloud-transaction</artifactId>
			<version>x.x.x.RELEASE</version>
		</dependency>

```
最新版本为 `2.2.0.RELEASE`

3. 添加tx.properties

```
#txmanager地址  http://txmanager ip:txmanager port/txmanager name/tx/manager/getServer 写法固定
url=http://127.0.0.1:8080/tx-manager-2.0.0-SNAPSHOT/tx/manager/getServer

#事务补偿记录配置

#日志类型 db file （db file二选一）
compensate.type=db
#db 数据库链接地址
compensate.db.url = jdbc:mysql://localhost:3306/test
#db 数据库类型 目前支持 mysql oracle sqlserver
compensate.db.dbType = mysql
#db 数据库账号
compensate.db.username = root
#db 数据库密码
compensate.db.password = root

#日志类型 db file
#compensate.type=file
# 文件日志地址
#compensate.file.path = /lcn

```

4. 添加事务拦截器
```java

@Aspect
@Component
public class TxTransactionInterceptor {

    @Autowired
    private TxManagerInterceptor txManagerInterceptor;

    @Around("execution(* com.example.demo.service.impl.*Impl.*(..))")
    public Object around(ProceedingJoinPoint point)throws Throwable{
        return txManagerInterceptor.around(point);
    }
}

```

注意：@Around 拦截地址不能包含com.lorne.tx.*

5. 在消费者配置拦截器.


**`@FeignClient`的方式** 

若使用的是`@FeignClient`的方式，则需要添加`configuration`配置。

方案一：

```java
@FeignClient(value = "demo2",configuration = TransactionRestTemplateConfiguration.class)
public interface Demo2Client {


    @RequestMapping("/demo/list")
    List<Test> list();


    @RequestMapping("/demo/save")
    int save();
}

```

直接使用lcn提供的`TransactionRestTemplateConfiguration`类。

方案二：

```java
@FeignClient(value = "demo3",configuration = MyConfiguration.class)
public interface Demo3Client {


    @RequestMapping("/demo/list")
    List<Test> list();


    @RequestMapping("/demo/save")
    int save();
}

```

```java
@Configuration
public class MyConfiguration {

    @Bean
    @Scope("prototype")
    public Feign.Builder feignBuilder() {
        return Feign.builder().requestInterceptor(new TransactionRestTemplateInterceptor());
    }
}

```

使用自定义Configuration添加`TransactionRestTemplateInterceptor`


**RestTemplate的方式**


在builder时添加拦截器

```java

	@Autowired
	private RestTemplateBuilder builder;

	@Bean
	public RestTemplate restTemplate() {
		return builder.interceptors(new TransactionHttpRequestInterceptor()).build();
	}


```
在RestTemplateBuilder添加`TransactionHttpRequestInterceptor`拦截器


**传统的Http请求方式**

若采用的是传统的Http请求那么需要手动在发起请求的header下添加tx-group参数如下：

```java

        TxTransactionLocal txTransactionLocal = TxTransactionLocal.current();
        String groupId = txTransactionLocal==null?null:txTransactionLocal.getGroupId();
        request.addHeader("tx-group",groupId);
        
```

支持传统的Http方式也就意味着支持http下的分布式事务

6. 配置项目的`application.properties`的配置文件。

```

spring.datasource.driver-class-name = com.mysql.jdbc.Driver
spring.datasource.url= jdbc:mysql://localhost:3306/test
spring.datasource.username= root
spring.datasource.password=root
spring.datasource.initialize =  true
init-db= true

spring.application.name = demo1
server.port = 8081
#${random.int[9000,9999]}
eureka.client.service-url.defaultZone=http://127.0.0.1:8761/eureka/

#Ribbon的负载均衡策略
nono.ribbon.NFLoadBalancerRuleClassName=com.netflix.loadbalancer.RandomRule


```

7. 创建数据库，项目都是依赖相同的数据库，创建一次其他的demo下将不再需要重复创建。mysql数据库，库名称test

```sql

USE test;

DROP TABLE IF EXISTS `t_test`;

CREATE TABLE `t_test` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;


```

## 注意事项

1. 禁止重名的bean对象。

事务的补偿机制是基于java反射的方式重新执行一次需要补偿的业务。因此执行的时候需要获取到业务的service对象，LCN是基于spring的ApplicationContent的getBean方法获取bean的对象的。因此不允许出现重名对象。


2. 分布式事务的方法下不能开启本地事务

本地事务与分布式事务是兼容的，但是由于分布式事务处理的时候是在本地事务基础上开启了一个线程处理的事务操作。这样做以后其实该业务方法完全可以不支持事务的，分布式事务在线程下处理了事务。在高并发的情况下这样的做法会耗尽链接池资源，因此做如下配置。

也是由于该原因，因此需要在所有参与分布式事务的业务模块上都要添加TxTransaction注解。注解只需要在模块的分布式事务开始方法上添加即可。

例如： 若存在A模块调用B模块的分布式事务配置，若A模块方法调用关系是A下的a1做为开始方法，调用了本地的a2 a3方法。然后在a4远程调用的B模块的b1方法，且B模块的调用关系也是由b1调用了b2 b3。 那么关于分布式事务注解的配置的时候只需要配置A模块的a1方法下，B模块的b1方法下。其他的方法均无须做任何处理。


## 测试说明


demo1/demo2类型:

运行demo2下的DemoApplication，再运行demo1下的DemoApplication。

然后在浏览器访问http://127.0.0.1:8081/demo/save

效果：/by zero 异常所有事务都回滚。

说明： demo1都是消费者，默认在业务里添加了`int v = 100/0;`异常代码。因此在不注释的情况下事务回归。


demo1/2/3/4/5类型:
 
运行demo5下的DemoApplication，再运行demo4下的DemoApplication，再运行demo3下的DemoApplication，再运行demo2下的DemoApplication，再运行demo1下的DemoApplication。

然后在浏览器访问http://127.0.0.1:8081/demo/save
 
效果：/by zero 异常所有事务都回滚。

说明：demo1和demo3是消费者，默认在业务里添加了`int v = 100/0;`，demo3这行已注释，默认回滚，全部注释掉会提交事务。


技术交流群：554855843
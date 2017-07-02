# springcloud LCN实例demo


## 使用说明

框架分为jdbc／jpa／mybatis三个版本。各个版本之间除了DB框架差异以外，其他配置都相同。

demo分为两类，demo1/demo2 只是消费者与提供者两个的简单demo。以及demo1/2/3/4/5复杂类型调用关系。

demo1/demo2类型：

demo1作为消费者（分布式事务的发起者）调用demo2.

demo1/2/3/4/5类型：

demo1作为分布式事务的发起者，调用了demo2 demo3，demo3有调用了demo4 demo5.

## 使用步骤

1. 启动TxManager
2. 添加配置maven库与tx-lcn库。

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
最新版本为 `2.0.0.RELEASE`

3. 添加tx.properties

```
#txmanager地址 /tx/manager/getServer写法固定
url=http://192.168.3.102:8888/tx/manager/getServer

#模块名称 （模块不能重名）
model=demo1
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

5. 消费者方`@FeignClient`下添加`configuration`配置。 

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

## 测试说明

demo1/demo2类型的 demo1都是消费者，默认在业务里添加了`int v = 100/0;`异常代码。因此在不注释的情况下事务回归。

demo1/2/3/4/5 下载demo1与demo3下都添加了`int v = 100/0;`，默认回滚，全部注释掉会提交事务。


技术交流群：554855843
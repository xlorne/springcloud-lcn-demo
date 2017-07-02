# springcloud-mybatis-demo1

springcloud-mybatis-demo1 事务发起方


### 使用步骤

项目依赖服务：  
redis服务   
mysql服务   
springcloud-eureka服务  
   

1. 启动springcloud-eureka服务.
1. 启动[txManager](https://github.com/1991wangliang/springcloud-tx-manager)服务.
2. 创建数据库test，执行init.sql脚本
3. 配置springcloud-mybatis-demo1和springcloud-mybatis-demo2的application.properties配置
3. 启动springcloud-mybatis-demo2的[DemoApplication.main()](https://github.com/1991wangliang/springcloud-mybatis-demo2)
4. 在浏览器下打开 http://127.0.0.1:8081/demo/save 地址。

### 注意事项
 
1. FeignClient客户端下添加configuration配置。(目前仅支持FeignClient方式远程访问)
 
 方案一使用框架提供的TransactionRestTemplateConfiguration配置
```$xslt
@FeignClient(value = "demo2",configuration = TransactionRestTemplateConfiguration.class)
public interface Demo2Client {
    

    @RequestMapping("/demo/list")
    List<Test> list();


    @RequestMapping("/demo/save")
    int save();
}

```
 方案二自定义configuration配置TransactionRestTemplateInterceptor事务拦截器
  
```$xslt

@Configuration
public class MyConfiguration {

    @Bean
    @Scope("prototype")
    public Feign.Builder feignBuilder() {
        return Feign.builder().requestInterceptor(new TransactionRestTemplateInterceptor());
    }
}


```
 
2. 自定义事务切面配置 @Around根据业务自由配置。 禁止配置全部包`* com.*.*.service.impl.*Impl.*(..))` 该写法将会拦截分布式事务框架本身，影响框架功能。
 
```$xslt
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

#### 效果：
会出异常，然后数据库数据不被修改。当注释调springcloud-mybatis-demo1 DemoServiceImpl下的 int v = 100/0;代码后，数据库下t_test表会增加两条数据。

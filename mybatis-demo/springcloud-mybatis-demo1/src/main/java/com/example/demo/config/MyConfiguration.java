package com.example.demo.config;

import com.lorne.tx.springcloud.feign.TransactionRestTemplateInterceptor;
import feign.Contract;
import feign.Feign;
import feign.RequestInterceptor;
import feign.auth.BasicAuthRequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * Created by lorne on 2017/6/28.
 */

@Configuration
public class MyConfiguration {

//    @Bean
//    @Scope("prototype")
//    public Feign.Builder feignBuilder() {
//        return Feign.builder().requestInterceptor(new TransactionRestTemplateInterceptor());
//    }

//    @Bean
//    public Contract feignContract() {
//        return new feign.Contract.Default();
//    }

//    @Bean
//    public BasicAuthRequestInterceptor basicAuthRequestInterceptor() {
//        return new BasicAuthRequestInterceptor("user", "password");
//    }

    @Bean
    public RequestInterceptor requestInterceptor(){
        return new TransactionRestTemplateInterceptor();
    }
}

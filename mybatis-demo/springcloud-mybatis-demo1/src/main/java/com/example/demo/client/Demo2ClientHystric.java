package com.example.demo.client;

import com.example.demo.entity.Test;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by fangzhipeng on 2017/4/6.
 */
@Component
public class Demo2ClientHystric implements Demo2Client {


    @Override
    public List<Test> list() {
        System.out.println("进入断路器-list。。。");
        return null;
    }

    @Override
    public int save() {
        System.out.println("进入断路器-save。。。");
        return 0;
    }
}

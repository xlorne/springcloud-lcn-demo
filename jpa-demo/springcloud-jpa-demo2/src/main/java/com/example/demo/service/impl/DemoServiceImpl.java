package com.example.demo.service.impl;

import com.example.demo.dao.TestRepository;
import com.example.demo.entity.Test;
import com.example.demo.service.DemoService;
import com.lorne.tx.annotation.TxTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by lorne on 2017/6/26.
 */
@Service
public class DemoServiceImpl implements DemoService {

    @Autowired
    private TestRepository testRepository;

    @Override
    public List<Test> list() {
        return testRepository.findAll();
    }

    @Override
    @TxTransaction
    public int save() {

        Test test = new Test();
        test.setName("hello2");
        int rs = testRepository.save(test).getId();

        return rs;
    }
}

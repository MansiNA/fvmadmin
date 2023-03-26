package com.example.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Random;

@Service
public class BackendService {
    @Autowired
    static
    JdbcTemplate jdbcTemplate;


    public void save(String name) {
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Async
    public ListenableFuture<Integer> saveAsync(String name) {
        int i=0;

           // Thread.sleep(10);
            Random random = new Random();
            i = random.nextInt(300);

        return AsyncResult.forValue(i);
    }



}

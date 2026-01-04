package com.qyl.v2trade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class V2TradeApplication {

    public static void main(String[] args) {

        SpringApplication.run(V2TradeApplication.class, args);
    }

}

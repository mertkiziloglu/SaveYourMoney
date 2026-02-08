package com.hackathon.greedy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GreedyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreedyServiceApplication.class, args);
    }
}

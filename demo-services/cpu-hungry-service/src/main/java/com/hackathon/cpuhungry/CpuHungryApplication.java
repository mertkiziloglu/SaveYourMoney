package com.hackathon.cpuhungry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CpuHungryApplication {

    public static void main(String[] args) {
        SpringApplication.run(CpuHungryApplication.class, args);
    }
}

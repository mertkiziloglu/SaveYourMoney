package com.hackathon.memoryleaker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MemoryLeakerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemoryLeakerApplication.class, args);
    }
}

package com.hackathon.dbconnection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DbConnectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbConnectionApplication.class, args);
    }
}

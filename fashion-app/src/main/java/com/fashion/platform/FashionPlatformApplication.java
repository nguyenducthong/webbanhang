package com.fashion.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FashionPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(FashionPlatformApplication.class, args);
    }
}


package com.bookmania.bookmania;

import com.bookmania.bookmania.Configuration.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
@EnableScheduling
public class BookManiaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookManiaApplication.class, args);
    }
}
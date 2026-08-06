package com.example.seoanalytics;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.example.seoanalytics.mapper")
@EnableScheduling
public class SeoAnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeoAnalyticsApplication.class, args);
    }
}

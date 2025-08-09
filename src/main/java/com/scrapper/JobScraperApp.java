package com.scrapper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.scrapper")
public class JobScraperApp {
    public static void main(String[] args) {
        SpringApplication.run(JobScraperApp.class, args);
    }
}
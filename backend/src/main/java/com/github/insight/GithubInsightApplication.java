package com.github.insight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class GithubInsightApplication {

    public static void main(String[] args) {
        SpringApplication.run(GithubInsightApplication.class, args);
    }
}

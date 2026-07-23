package com.ci.pipeline.service.bootstrap;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CI Pipeline Server 启动类
 */
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = {"com.ci.pipeline"})
@EnableFeignClients(basePackages = "com.ci.pipeline.service.remote")
@MapperScan(basePackages = "com.ci.pipeline.dao.mapper")
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

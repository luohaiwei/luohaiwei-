package com.lab.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 实验室设备预约系统启动类
 * 基于SpringBoot 2.7.x
 * 整合Spring Security、MyBatis-Plus、Redis
 */
@SpringBootApplication
@EnableScheduling
public class ReservationApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReservationApplication.class, args);
    }
}

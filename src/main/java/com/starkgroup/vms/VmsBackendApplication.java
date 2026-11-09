package com.starkgroup.vms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VmsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(VmsBackendApplication.class, args);
    }
}

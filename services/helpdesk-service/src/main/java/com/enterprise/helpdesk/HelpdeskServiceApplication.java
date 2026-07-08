package com.enterprise.helpdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.enterprise.helpdesk", "com.enterprise.shared"})
public class HelpdeskServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelpdeskServiceApplication.class, args);
    }
}

package com.ivedi.ivedikuryemanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class IvediKuryeManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(IvediKuryeManagementApplication.class, args);
    }

}

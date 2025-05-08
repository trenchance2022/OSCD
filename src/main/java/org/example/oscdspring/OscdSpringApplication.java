package org.example.oscdspring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OscdSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(OscdSpringApplication.class, args);
    }

}

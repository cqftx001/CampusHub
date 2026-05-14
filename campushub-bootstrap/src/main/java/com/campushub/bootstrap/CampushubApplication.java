package com.campushub.bootstrap;

import com.campushub.bootstrap.config.CorsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableConfigurationProperties(CorsProperties.class)
@SpringBootApplication(scanBasePackages = "com.campushub")
@EnableJpaAuditing
public class CampushubApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampushubApplication.class, args);
    }
}

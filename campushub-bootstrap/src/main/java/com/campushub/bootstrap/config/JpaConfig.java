package com.campushub.bootstrap.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "com.campushub")
@EnableJpaRepositories(basePackages = "com.campushub")
public class JpaConfig {
}

package com.campushub.auth.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@EntityScan(basePackages = "com.campushub.auth")
@EnableJpaRepositories(basePackages = "com.campushub.auth")
@EnableConfigurationProperties({
        EmailVerificationProperties.class,
        JwtProperties.class,
        LoginSessionProperties.class
})
public class AuthModuleConfiguration {

}

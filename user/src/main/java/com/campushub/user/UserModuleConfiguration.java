package com.campushub.user;

import com.campushub.user.domain.UserProfile;
import com.campushub.user.repository.UserProfileRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackageClasses = UserProfile.class)
@EnableJpaRepositories(basePackageClasses = UserProfileRepository.class)
public class UserModuleConfiguration {
}

package com.campushub.marketplace.config;

import com.campushub.marketplace.domain.Listing;
import com.campushub.marketplace.repository.ListingRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = Listing.class)
@EnableJpaRepositories(basePackageClasses = ListingRepository.class)
public class MarketplaceModuleConfiguration {
}
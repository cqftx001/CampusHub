package com.campushub.marketplace.config;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.Protocol;
import com.aliyun.oss.common.comm.SignVersion;
import com.campushub.marketplace.domain.Listing;
import com.campushub.marketplace.repository.ListingRepository;
import com.campushub.marketplace.storage.AlibabaCloudOssImageStorage;
import com.campushub.marketplace.storage.MarketplaceImageStorage;
import com.campushub.marketplace.storage.UnavailableMarketplaceImageStorage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = Listing.class)
@EnableJpaRepositories(basePackageClasses = ListingRepository.class)
@EnableConfigurationProperties(OssProperties.class)
public class MarketplaceModuleConfiguration {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(
            prefix = "campushub.oss",
            name = "enabled",
            havingValue = "true"
    )
    public OSS marketplaceOssClient(OssProperties properties) {
        properties.validateEnabledConfiguration();

        ClientBuilderConfiguration configuration = new ClientBuilderConfiguration();
        configuration.setProtocol(Protocol.HTTPS);
        configuration.setSignatureVersion(SignVersion.V4);

        return OSSClientBuilder.create()
                .endpoint(properties.endpoint())
                .region(properties.region())
                .credentialsProvider(new DefaultCredentialProvider(
                        properties.accessKeyId(),
                        properties.accessKeySecret()
                ))
                .clientConfiguration(configuration)
                .build();
    }

    @Bean
    public MarketplaceImageStorage marketplaceImageStorage(
            OssProperties properties,
            ObjectProvider<OSS> ossClientProvider
    ) {
        if (!properties.enabled()) {
            return new UnavailableMarketplaceImageStorage();
        }

        return new AlibabaCloudOssImageStorage(
                ossClientProvider.getObject(),
                properties.bucketName(),
                properties.normalizedPublicBaseUrl()
        );
    }
}

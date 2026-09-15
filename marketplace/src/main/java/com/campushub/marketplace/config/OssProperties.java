package com.campushub.marketplace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "campushub.oss")
public record OssProperties(
        boolean enabled,
        String endpoint,
        String region,
        String accessKeyId,
        String accessKeySecret,
        String bucketName,
        String publicBaseUrl
) {

    public void validateEnabledConfiguration() {
        requireText(endpoint, "campushub.oss.endpoint");
        requireText(region, "campushub.oss.region");
        requireText(accessKeyId, "campushub.oss.access-key-id");
        requireText(accessKeySecret, "campushub.oss.access-key-secret");
        requireText(bucketName, "campushub.oss.bucket-name");
        requireText(publicBaseUrl, "campushub.oss.public-base-url");

        if (!publicBaseUrl.startsWith("https://")) {
            throw new IllegalStateException(
                    "campushub.oss.public-base-url must use HTTPS"
            );
        }
    }

    public String normalizedPublicBaseUrl() {
        return publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    private static void requireText(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(propertyName + " must be configured when OSS is enabled");
        }
    }

    @Override
    public String toString() {
        return "OssProperties[enabled=" + enabled
                + ", endpoint=" + endpoint
                + ", region=" + region
                + ", accessKeyId=<redacted>"
                + ", accessKeySecret=<redacted>"
                + ", bucketName=" + bucketName
                + ", publicBaseUrl=" + publicBaseUrl
                + "]";
    }
}

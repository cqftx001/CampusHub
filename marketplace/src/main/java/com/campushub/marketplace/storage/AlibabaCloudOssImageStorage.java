package com.campushub.marketplace.storage;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.campushub.marketplace.error.MarketplaceErrorCode;
import com.campushub.marketplace.error.MarketplaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.UUID;

public class AlibabaCloudOssImageStorage implements MarketplaceImageStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlibabaCloudOssImageStorage.class);

    private final OSS ossClient;
    private final String bucketName;
    private final String publicBaseUrl;

    public AlibabaCloudOssImageStorage(
            OSS ossClient,
            String bucketName,
            String publicBaseUrl
    ) {
        this.ossClient = ossClient;
        this.bucketName = bucketName;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public StoredMarketplaceImage store(
            UUID accountId,
            UUID uploadBatchId,
            UUID imageId,
            String extension,
            String contentType,
            long contentLength,
            InputStream content
    ) {
        String objectKey = createObjectKey(
                accountId,
                uploadBatchId,
                imageId,
                extension
        );

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(contentLength);
        metadata.setCacheControl("public, max-age=31536000, immutable");

        try {
            ossClient.putObject(bucketName, objectKey, content, metadata);
            return new StoredMarketplaceImage(
                    objectKey,
                    publicBaseUrl + "/" + objectKey
            );
        } catch (OSSException exception) {
            LOGGER.error(
                    "OSS rejected marketplace image upload; errorCode={}, requestId={}",
                    exception.getErrorCode(),
                    exception.getRequestId()
            );
            throw storageUnavailable();
        } catch (ClientException exception) {
            LOGGER.error(
                    "OSS client failed marketplace image upload; errorCode={}, requestId={}",
                    exception.getErrorCode(),
                    exception.getRequestId()
            );
            throw storageUnavailable();
        }
    }

    @Override
    public void deleteBestEffort(String objectKey) {
        try {
            ossClient.deleteObject(bucketName, objectKey);
        } catch (OSSException exception) {
            LOGGER.warn(
                    "Could not clean up OSS marketplace image; objectKey={}, errorCode={}, requestId={}",
                    objectKey,
                    exception.getErrorCode(),
                    exception.getRequestId()
            );
        } catch (ClientException exception) {
            LOGGER.warn(
                    "ClientException: Could not clean up OSS marketplace image; objectKey={}, errorCode={}, requestId={}",
                    objectKey,
                    exception.getErrorCode(),
                    exception.getRequestId()
            );
        }
    }

    private String createObjectKey(
            UUID accountId,
            UUID uploadBatchId,
            UUID imageId,
            String extension
    ) {
        return "marketplace/accounts/%s/%s/%s.%s".formatted(
                accountId,
                uploadBatchId,
                imageId,
                extension
        );
    }

    private MarketplaceException storageUnavailable() {
        return new MarketplaceException(MarketplaceErrorCode.IMAGE_STORAGE_UNAVAILABLE);
    }
}

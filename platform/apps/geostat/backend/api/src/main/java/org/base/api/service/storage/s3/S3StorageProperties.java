package org.base.api.service.storage.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** S3-compatible storage binding. Every value comes from the environment; nothing names a site. */
@ConfigurationProperties("storage.s3")
public record S3StorageProperties(String endpoint, String accessKey, String secretKey, String ingestBucket, String quarantineBucket,
                                  String archiveBucket, String exportBucket, String publicEndpoint, String region, String stagingPrefix) {

    public boolean distributionConfigured() {
        return publicEndpoint != null && !publicEndpoint.isBlank();
    }
}

package org.base.api.service.storage.s3;

import io.minio.MinioClient;

import java.util.Optional;

/**
 * @param internal     client for the endpoint the platform itself reaches
 * @param distribution client that signs URLs for the host a consumer reaches; empty disables distribution (fail closed)
 */
public record S3Clients(MinioClient internal, Optional<MinioClient> distribution) {

    public static S3Clients from(S3StorageProperties properties) {
        MinioClient internal = MinioClient.builder().endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey()).build();
        /* An explicit region keeps signing offline; otherwise the SDK looks it up over the network. */
        Optional<MinioClient> distribution = properties.distributionConfigured()
                ? Optional.of(MinioClient.builder().endpoint(properties.publicEndpoint())
                        .credentials(properties.accessKey(), properties.secretKey()).region(properties.region()).build())
                : Optional.empty();
        return new S3Clients(internal, distribution);
    }
}

package org.base.api.service.storage.s3;

import org.base.api.service.artifact.ArtifactObjectInventory;
import org.base.api.service.artifact.ArtifactObjectStore;
import org.base.api.service.artifact.ArtifactUploadStagingStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Binds the storage ports to S3 when an endpoint is configured. Another provider is another configuration. */
@Configuration
@EnableConfigurationProperties(S3StorageProperties.class)
@ConditionalOnExpression("'${storage.s3.endpoint:}'.length() > 0")
public class S3StorageConfiguration {

    @Bean
    S3Clients s3Clients(S3StorageProperties properties) {
        return S3Clients.from(properties);
    }

    @Bean
    ArtifactObjectStore artifactObjectStore(S3Clients clients, S3StorageProperties properties) {
        return new S3ArtifactObjectStore(clients, properties.ingestBucket());
    }

    @Bean
    ArtifactUploadStagingStore artifactUploadStagingStore(S3Clients clients, ArtifactObjectStore objects, S3StorageProperties properties) {
        return new S3ArtifactUploadStagingStore(clients.internal(), objects, properties.stagingPrefix());
    }

    @Bean
    ArtifactObjectInventory artifactObjectInventory(S3Clients clients) {
        return new S3ArtifactObjectInventory(clients.internal());
    }
}

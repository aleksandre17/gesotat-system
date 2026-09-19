package org.base.api.service.storage.s3;

import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import org.base.api.service.artifact.ArtifactObjectInventory;
import org.base.api.service.artifact.ArtifactStorageException;

import java.util.ArrayList;
import java.util.List;

public class S3ArtifactObjectInventory implements ArtifactObjectInventory {
    private final MinioClient client;

    public S3ArtifactObjectInventory(MinioClient client) {
        this.client = client;
    }

    @Override
    public List<ListedObject> page(String bucket, String prefix, String afterKey, int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit must be positive");
        ListObjectsArgs.Builder args = ListObjectsArgs.builder().bucket(bucket).prefix(prefix).recursive(true).maxKeys(limit);
        if (afterKey != null) args.startAfter(afterKey);
        List<ListedObject> page = new ArrayList<>(limit);
        try {
            for (Result<Item> result : client.listObjects(args.build())) {
                Item item = result.get();
                if (item.isDir()) continue;
                page.add(new ListedObject(item.objectName(), item.size(), item.lastModified().toInstant()));
                if (page.size() == limit) break;
            }
            return page;
        } catch (Exception error) {
            throw new ArtifactStorageException("Object listing failed", error);
        }
    }
}

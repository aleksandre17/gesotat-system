package org.base.api.service.storage;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Ensures every private platform bucket exists before any import can be accepted. */
@Component
public class PlatformStorageProvisioner implements ApplicationRunner {
    private final ObjectProvider<ObjectStorageService> storage;
    public PlatformStorageProvisioner(ObjectProvider<ObjectStorageService> storage) { this.storage=storage; }
    @Override public void run(ApplicationArguments args) throws Exception {
        ObjectStorageService configured=storage.getIfAvailable();
        if(configured!=null) configured.provision();
    }
}

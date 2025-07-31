package org.base.api.model.request;

import lombok.Data;

import java.util.List;

@Data
public class UploadPayload {
    private List<String> years;
    private boolean clearServerData;
    private String taskId;
    private String metaDatabaseUrl;
    private String metaDatabaseUser;
    private String metaDatabasePassword;
    private String metaDatabaseName;
    private String metaDatabaseType;
}

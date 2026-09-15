package org.base.core.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.base.core.entity.data.DataKind;
import org.base.core.entity.data.DataMode;

@Data
public class DataProfileRequest {
    @NotNull
    private Long pageId;
    @NotBlank
    private String profileCode;
    @NotNull
    private DataMode dataMode;
    @NotNull
    private DataKind dataKind;
    @NotBlank
    private String targetDatabase;
    @NotBlank
    private String targetSchema;
    @NotBlank
    private String targetTable;
    private String rowKeyJson;
    private String displayColumnsJson;
    private String allowedQueryJson;
    private Boolean enabled = true;
}

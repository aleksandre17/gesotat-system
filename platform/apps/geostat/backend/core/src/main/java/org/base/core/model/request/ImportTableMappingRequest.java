package org.base.core.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.base.core.entity.data.ImportMode;
import org.base.core.entity.data.TableRole;

@Data
public class ImportTableMappingRequest {
    @NotNull private Long profileId;
    @NotBlank private String accessTableName;
    @NotNull private TableRole tableRole;
    @NotNull private ImportMode importMode;
    private String columnMappingJson;
    private String validationRulesJson;
    private Boolean enabled = true;
}

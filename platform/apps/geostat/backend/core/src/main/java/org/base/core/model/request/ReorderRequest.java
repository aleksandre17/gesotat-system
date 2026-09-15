package org.base.core.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReorderRequest {

    @NotNull
    private Long pageId;
    private Long parentId;
    private int orderIndex;
}
package org.base.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface PageNodeMixIn {
    @JsonProperty("parentId")
    Long getParentId();

    @JsonProperty("parentId")
    void setParentId(Long parentId);
}

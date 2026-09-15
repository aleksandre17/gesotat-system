package org.base.core.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class NodeRequest {

    @NotBlank
    private String name;
    private String slug;
    private String icon;
    private String nodeType;
    private Boolean isFolder;
    private Integer orderIndex;
    private ParentRef parent;

    // DIRECTORY-specific
    private String description;
    private AccessControlRequest accessControl;

    // PAGE-specific
    private String resource;
    private String metaTitle;
    private String metaDescription;
    private String metaDatabaseType;
    private String metaDatabaseUrl;
    private String metaDatabaseUser;
    private String metaDatabasePassword;
    private String metaDatabaseName;
}
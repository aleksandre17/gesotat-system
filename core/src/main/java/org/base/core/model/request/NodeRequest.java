package org.base.core.model.request;

import lombok.Data;

import java.util.List;

@Data
public class NodeRequest {
    private String name;
    private String description;
    private Boolean isFolder;
    private String nodeType;
    private Integer level;  // Current level in the hierarchy

    // Common fields
    private Integer sortOrder;
    private List<NodeRequest> children;


    // Page-specific fields
    private String resource;
    private String metaDatabaseType;
    private String metaDatabaseUrl;
    private String metaDatabaseUser;
    private String metaDatabasePassword;
    private String metaDatabaseName;
    private String metaTitle;
    private String metaDescription;
    private String slug;
    private Long parentId;
    private String icon;
}


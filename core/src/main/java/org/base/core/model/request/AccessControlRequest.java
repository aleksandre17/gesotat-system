package org.base.core.model.request;

import lombok.Data;

@Data
public class AccessControlRequest {

    private Long userId;
    private Long roleId;
    private Long permissionId;
}
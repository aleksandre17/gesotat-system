package org.base.core.model.response;

import org.base.core.entity.User;
import java.util.List;

public record UserResponse(
        Long id,
        String username,
        List<RoleInfo> roles
) {
    public record RoleInfo(Long id, String name) {}

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRoles().stream()
                        .map(r -> new RoleInfo(r.getId(), r.getName()))
                        .toList()
        );
    }
}


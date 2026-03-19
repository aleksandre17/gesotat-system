package org.base.core.model.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UserRequest(
        @NotBlank(message = "Username is required")
        String username,

        @Email(message = "Invalid email format")
        //@NotBlank(message = "Email is required")
        String email,

        String password, // blank = keep existing (update only)

        List<RoleRef> roles
) {
    public record RoleRef(Long id) {}
}

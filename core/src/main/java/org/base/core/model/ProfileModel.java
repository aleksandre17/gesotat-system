package org.base.core.model;

public record ProfileModel(
        String username,      // from users.username — read-only
        String displayName,   // from profile.display_name — editable
        String avatar         // from profile.avatar — editable
) {}

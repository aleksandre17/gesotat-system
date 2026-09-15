package org.base.core.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignResponse {
    private String token;
    private String refreshToken;

    private List<Map<String, Object>> roles;
}

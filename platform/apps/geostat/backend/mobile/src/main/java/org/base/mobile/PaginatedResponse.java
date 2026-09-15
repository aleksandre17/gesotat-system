package org.base.mobile;

import java.util.List;

/**
 * Generic DTO for paginated responses.
 */
public record PaginatedResponse<T>(
        List<T> result,
        int currentPage,
        int totalPages,
        String totalResults
) {
}

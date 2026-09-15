package org.base.mobile;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Immutable context extracted from {@link HttpServletRequest} attributes.
 * <p>
 * Centralizes the inconsistent {@code request.getAttribute("langName")} vs
 * {@code request.getAttribute("lang")} pattern used across all controller endpoints.
 * <p>
 * The language filter sets both:
 * <ul>
 *   <li>{@code lang}     — raw locale key: "en", "ka"</li>
 *   <li>{@code langName} — column name:    "name_en", "name_ka"</li>
 *   <li>{@code period}   — period column:  "period_en", "period_ka"</li>
 *   <li>{@code title}    — title column:   "title_en", "title_ka"</li>
 * </ul>
 */
public record RequestContext(
        String lang,
        String langName,
        String period,
        String title
) {
    /**
     * Factory — extracts all language-related attributes from the request in one place.
     */
    public static RequestContext from(HttpServletRequest request) {
        return new RequestContext(
                attr(request, "lang"),
                attr(request, "langName"),
                attr(request, "period"),
                attr(request, "title")
        );
    }

    private static String attr(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value instanceof String s ? s : null;
    }
}


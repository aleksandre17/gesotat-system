package org.base.mobile.params;

import jakarta.validation.constraints.NotBlank;

public class QueryParams<T> {
    @NotBlank
    private final String langName;
    private String queryType;
    private final T params;

    public QueryParams(String langName, T params) {
        this.langName = langName;
        this.params = params;
    }

    public QueryParams(String langName, T params, String queryType) {
        this.langName = langName;
        this.params = params;
        this.queryType = queryType;
    }

    public String getLangName() { return langName; }
    public T getParams() { return params; }
    public String getQueryType() { return queryType; }
    public void setQueryType(String queryType) { this.queryType = queryType; }
}

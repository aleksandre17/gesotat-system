package org.base.api.service.mobile_services.dto;

import java.util.List;

public class WrappedResponseDTO<T> {
    private String name;
    private List<T> data;

    public WrappedResponseDTO(String name, List<T> data) {
        this.name = name;
        this.data = data;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<T> getData() { return data; }
    public void setData(List<T> data) { this.data = data; }
}

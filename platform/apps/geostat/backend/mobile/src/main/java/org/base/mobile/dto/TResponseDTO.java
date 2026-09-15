package org.base.mobile.dto;

import lombok.Data;

import java.util.List;

@Data
public class TResponseDTO {
    private String name;
    private List<TDTO> data;

    public TResponseDTO(String name, List<TDTO> data) {
        this.name = name;
        this.data = data;
    }

}

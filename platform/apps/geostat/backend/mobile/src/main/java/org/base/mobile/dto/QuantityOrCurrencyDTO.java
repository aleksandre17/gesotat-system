package org.base.mobile.dto;

import java.util.List;

public record QuantityOrCurrencyDTO(
        String name,
        int pointStart,
        List<Long> data
) {
    public QuantityOrCurrencyDTO {
        if (name == null) throw new IllegalArgumentException("Name cannot be null");
        if (data == null) throw new IllegalArgumentException("Data cannot be null");
    }
}

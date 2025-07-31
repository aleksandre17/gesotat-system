package org.base.api.service.mobile_services.params.text;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CompareParams {
    private String tableName;
    private String lang;
    @Nullable
    private String brand1;
    @Nullable
    private String brand2;
    @Nullable
    private String model1;
    @Nullable
    private String model2;
    @Nullable
    private String yearOfProd1;
    @Nullable
    private String yearOfProd2;

}

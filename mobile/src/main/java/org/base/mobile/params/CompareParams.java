package org.base.mobile.params;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class CompareParams {
    private final String brand1;
    private final String model1;
    private final String yearOfProduction1;
    private final String brand2;
    private final String model2;
    private final String yearOfProduction2;

    public CompareParams(String brand1, String model1, String yearOfProduction1,
                         String brand2, String model2, String yearOfProduction2) {
        this.brand1 = brand1;
        this.model1 = model1;
        this.yearOfProduction1 = yearOfProduction1;
        this.brand2 = brand2;
        this.model2 = model2;
        this.yearOfProduction2 = yearOfProduction2;
    }

    public String getBrand1() { return brand1; }
    public String getModel1() { return model1; }
    public String getYearOfProduction1() { return yearOfProduction1; }
    public String getBrand2() { return brand2; }
    public String getModel2() { return model2; }
    public String getYearOfProduction2() { return yearOfProduction2; }
}

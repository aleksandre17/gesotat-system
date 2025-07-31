package org.base.api.service.mobile_services.dto;

/**
 * DTO for license-dual endpoint response.
 */
public class LicenseDualDTO {
    private final String name;
    private final int data1; // From licenses_eoy
    private final int data2; // From licenses_main

    public LicenseDualDTO(String name, int data1, int data2) {
        this.name = name;
        this.data1 = data1;
        this.data2 = data2;
    }

    public String getName() {
        return name;
    }

    public int getData1() {
        return data1;
    }

    public int getData2() {
        return data2;
    }
}

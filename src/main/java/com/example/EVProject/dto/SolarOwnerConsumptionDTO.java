package com.example.EVProject.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SolarOwnerConsumptionDTO {

    private String ownerName;
    private Double totalConsumption;

    public SolarOwnerConsumptionDTO(String ownerName, Double totalConsumption) {
        this.ownerName = ownerName;
        this.totalConsumption = totalConsumption;
    }

}

package com.example.EVProject.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EnergyIntervalDTO {
    private Integer readingId;
    private LocalDateTime timestamp;
    private Double activePower;  // in Watts
    private Double avgPower;     // in Watts
    private Double deltaTimeHours;
    private Double energyWh;
    private Double cumulativeEnergyWh;
    private Double cumulativeEnergyKwh;
    private Double voltage;
    private Double current;
}
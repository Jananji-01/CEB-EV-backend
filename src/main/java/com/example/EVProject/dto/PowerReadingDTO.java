package com.example.EVProject.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PowerReadingDTO {
    private Long id;
    private LocalDateTime timestamp;
    private Double activePower;
    private Double energyWh;
    private Double cumulativeEnergyKwh;
}
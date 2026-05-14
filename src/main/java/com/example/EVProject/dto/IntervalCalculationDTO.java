package com.example.EVProject.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class IntervalCalculationDTO {
    private Integer readingId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double avgPower;
    private Double timeDeltaHours;
    private Double energyWh;
    private Double cumulativeKwh;
}
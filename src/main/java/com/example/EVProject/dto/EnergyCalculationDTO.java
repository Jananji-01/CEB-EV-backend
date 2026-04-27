package com.example.EVProject.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EnergyCalculationDTO {
    private Double totalEnergyKwh;
    private String calculationMethod;
    private Integer totalReadings;
    private LocalDateTime calculationTime;
    private List<IntervalCalculationDTO> sampleIntervals;
}
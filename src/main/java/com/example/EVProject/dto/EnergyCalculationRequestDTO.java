package com.example.EVProject.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EnergyCalculationRequestDTO {
    private String idDevice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long meterStart;
    private Long meterEnd;
}
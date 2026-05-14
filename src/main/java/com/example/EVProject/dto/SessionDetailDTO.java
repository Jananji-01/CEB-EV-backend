package com.example.EVProject.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SessionDetailDTO {
    private Integer sessionId;
    private String idDevice;
    private String eAccountNo;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double totalConsumption;
    private Double amount;
    private String status;
    private String chargingMode;
    private Double startSoc;
    private Double endSoc;
    private List<PowerReadingDTO> powerReadings;
    private EnergyCalculationDTO energyCalculation;
}
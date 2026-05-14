package com.example.EVProject.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SessionWithEnergyDTO {
    private Integer sessionId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String idDevice;
    private String eAccountNo;
    private Double soc;
    private String chargingMode;
    private Double totalConsumption;
    private Double amount;
    private String status;
    private Double totalEnergyKwh;
    private Long meterStart;
    private Long meterEnd;
    private List<EnergyIntervalDTO> energyIntervals;
}
package com.example.EVProject.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SessionReportDTO {
    private Integer sessionId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String idDevice;
    private String eAccountNo;
    private Double totalEnergyKwh;
    private Double totalConsumption;
    private Double amount;
    private String status;
    private Integer meterValueCount;
    private LocalDateTime firstMeterValue;
    private LocalDateTime lastMeterValue;
}
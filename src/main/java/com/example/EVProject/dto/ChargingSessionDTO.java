package com.example.EVProject.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChargingSessionDTO {

    private Integer sessionId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double soc;
    private String chargingMode;
    private Double totalConsumption;
    private Double amount;
    private String idDevice;
    private String evOwnerAccountNo; 
    private Long meterStart;
    
    // Optional: Add status field if needed for frontend
    private String status;  // This can be derived from endTime being null or not
    
    public boolean isActive() {
        return "ACTIVE".equals(status) || (endTime == null && status == null);
    }
    
    // Helper method to get duration in minutes
    public Long getDurationMinutes() {
        if (startTime == null) return 0L;
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).toMinutes();
    }

    public Double getCalculatedConsumption(Long meterStop) {
        if (meterStart != null && meterStop != null) {
            return (double) (meterStop - meterStart);
        }
        return totalConsumption;
    }
}
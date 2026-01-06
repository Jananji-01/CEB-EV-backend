package com.example.EVProject.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MonthlyConsumptionResponse {
    private String username;
    private String accountNumber;
    private String idDevice;
    private Integer month;
    private Integer year;

    // frontend uses this
    private Double totalConsumption;

    // extra (good for later)
    private Integer totalSessions;
    private Integer totalDurationMinutes;
}

package com.example.EVProject.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MonthlyConsumptionResponse {
    private String username;
    private String eAccountNumber;
    private String idDevice;
    private Integer month;
    private Integer year;

    // frontend uses these fields to display the data, they are calculated in the service layer 
    private Double totalConsumption;
    private Integer totalSessions;
    private Integer totalDurationMinutes;
    private Double totalAmount;
}

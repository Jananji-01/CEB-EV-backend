package com.example.EVProject.dto;

import lombok.Data;

@Data
public class MonthlyConsumptionRequest {
    private String username;
    private String eAccountNumber;

    // optional if you want select month later
    private Integer month;
    private Integer year;
}

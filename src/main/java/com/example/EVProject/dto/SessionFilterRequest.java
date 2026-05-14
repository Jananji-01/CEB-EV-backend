package com.example.EVProject.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SessionFilterRequest {
    private String deviceId;
    private String eAccountNo;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
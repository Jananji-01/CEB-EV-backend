package com.example.EVProject.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AssignSmartPlugRequestDTO {
    private String assignmentType;   // "new" or "existing"
    // For new station
    private String stationName;
    private String accountNumber;    // for new station, to link solar owner
    private Double latitude;
    private Double longitude;
    private Double solarPowerAvailable; // optional
    // For existing station
    private Integer existingStationId;
}
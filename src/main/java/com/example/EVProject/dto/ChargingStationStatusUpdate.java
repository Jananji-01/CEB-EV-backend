package com.example.EVProject.dto;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ChargingStationStatusUpdate {
    private Integer stationId;
    private Integer status; // this holds the status code: 1, 2, or 3
    private String errorCode;
    private LocalDateTime timestamp;
}

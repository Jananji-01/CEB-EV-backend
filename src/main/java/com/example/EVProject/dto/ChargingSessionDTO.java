package com.example.EVProject.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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




    // getters and setters

}

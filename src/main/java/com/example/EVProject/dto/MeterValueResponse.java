package com.example.EVProject.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MeterValueResponse {
    private Long meterId;
    private Integer connectorId;
    private LocalDateTime timestamp;
    private String idDevice;
    private Integer stationId;
    private List<SampleData> sampledValues;

    public void setIdDevice(String idDevice) {
        this.idDevice = idDevice;
    }

    @Data
    public static class SampleData {
        private Double value;
        private String context;
        private String format;
        private String measurand;
        private String location;
        private String unit;
    }
}

package com.example.EVProject.dto;

import lombok.Data;
import java.util.List;

@Data
public class MeterValueRequest {
    private Integer connectorId;
    private Integer transactionId; // maps to session_id
    private List<MeterReading> meterValue;

    @Data
    public static class MeterReading {
        private String timestamp;
        private List<SampleReading> sampledValue;
    }

    @Data
    public static class SampleReading {
        private String value;
        private String context;
        private String format;
        private String measurand;
        private String location;
        private String unit;
    }
}

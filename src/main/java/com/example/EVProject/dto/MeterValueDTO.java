package com.example.EVProject.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeterValueDTO {
    private String timestamp;
    private List<SampledValueDTO> sampledValue;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SampledValueDTO {
        private String value;
        private String measurand;
        private String unit;
        private String context;
        private String format;
        private String phase;
        private String location;
    }
}
package com.example.EVProject.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionEventDTO {
    private String eventType;
    private String timestamp;
    private TransactionDataDTO transactionData;
    private MeterValueDTO meterValue;

    @Data
    public static class TransactionDataDTO {
        private String transactionId;
        private String idToken;
        private Double chargingState;
    }
}
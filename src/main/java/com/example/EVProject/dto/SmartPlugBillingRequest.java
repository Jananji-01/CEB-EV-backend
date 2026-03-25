package com.example.EVProject.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmartPlugBillingRequest {

    @JsonProperty("PlugOwner_AccountNo")
    private String plugOwnerAccountNo;

    @JsonProperty("ElectricVehicle_AccountNo")
    private String electricVehicleAccountNo;

    @JsonProperty("Charging_StartTime")
    private String chargingStartTime;

    @JsonProperty("Charging_EndTime")
    private String chargingEndTime;

    @JsonProperty("kWh_Utilised")
    private Double kWhUtilised;

    @JsonProperty("PlugDeviceID")
    private Integer plugDeviceID;  

    @JsonProperty("PlugOwner_Active")
    private Integer plugOwnerActive;

    @JsonProperty("PlugLocation")
    private String plugLocation;

    @JsonProperty("CreateOn")
    private String createOn;

    @JsonProperty("CreatedBy")
    private String createdBy;
}
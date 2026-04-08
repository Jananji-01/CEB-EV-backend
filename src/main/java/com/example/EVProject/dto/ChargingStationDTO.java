package com.example.EVProject.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter

public class ChargingStationDTO {

    private Integer stationId;
    private Double latitude;
    private Double longitude;
    private String stationName;
    private Double solarPowerAvailable;
    private String status;
    private String errorCode;
    private LocalDateTime timestamp; 
    private Integer solarOwnerId;
    private String idDevice;

    // getters and setters

    public Integer getStationId() {
        return stationId;
    }

    public void setStationId(Integer stationId) {
        this.stationId = stationId;
    }

    public String getStationName() { 
        return stationName; 
    }

    public void setStationName(String stationName) { 
        this.stationName = stationName; 
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getSolarPowerAvailable() {
        return solarPowerAvailable;
    }

    public void setSolarPowerAvailable(Double solarPowerAvailable) {
        this.solarPowerAvailable = solarPowerAvailable;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSolarOwnerId() {
        return solarOwnerId;
    }

    public void setSolarOwnerId(Integer solarOwnerId) {
        this.solarOwnerId = solarOwnerId;
    }

    public LocalDateTime getTimestamp() { 
        return timestamp; 
    }

    public void setTimestamp(LocalDateTime timestamp) { 
        this.timestamp = timestamp; 
    }

    public String getIdDevice() { 
        return idDevice; 
    }

    public void setIdDevice(String idDevice) { 
        this.idDevice = idDevice; 
    }
}

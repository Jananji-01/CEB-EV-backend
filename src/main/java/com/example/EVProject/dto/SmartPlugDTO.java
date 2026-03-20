//package com.example.EVProject.dto;
//
//import lombok.Getter;
//import lombok.Setter;
//
//@Getter
//@Setter
//
//public class SmartPlugDTO {
//
//    private String idDevice;
//    private String cebSerialNo;
//    private Double maximumOutput;
//    private Integer stationId;
//
//    // getters and setters
//
//    public String getDeviceId() {
//        return idDevice;
//    }
//
//    public void setDeviceId(String deviceId) {
//        this.idDevice = idDevice;
//    }
//
//    public String getCebSerialNo() {
//        return cebSerialNo;
//    }
//
//    public void setCebSerialNo(String cebSerialNo) {
//        this.cebSerialNo = cebSerialNo;
//    }
//
//    public Double getMaximumOutput() {
//        return maximumOutput;
//    }
//
//    public void setMaximumOutput(Double maximumOutput) {
//        this.maximumOutput = maximumOutput;
//    }
//
//    public Integer getStationId() {
//        return stationId;
//    }
//
//    public void setStationId(Integer stationId) {
//        this.stationId = stationId;
//    }
//}


package com.example.EVProject.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class SmartPlugDTO {
    
    // Basic identification
    private String idDevice;
    private String cebSerialNo;
    private Double maximumOutput;
    private Integer stationId;
    
    // Charging point information (for registration)
    private String chargePointModel;
    private String chargePointVendor;
    private String firmwareVersion;
    
    // QR code information
    private String qrCodeData;
    private LocalDateTime qrCodeGeneratedAt;
    
    // Additional fields for display/management
    private String status;
    private LocalDateTime lastSeen;
    private String location;
    
    // For registration response
    private Boolean registrationSuccess;
    private String message;

    // No need for explicit getters and setters since we have Lombok annotations
    // But if you need them (for compatibility), here they are:

    public String getIdDevice() {
        return idDevice;
    }

    public void setIdDevice(String idDevice) {
        this.idDevice = idDevice;
    }

    public String getCebSerialNo() {
        return cebSerialNo;
    }

    public void setCebSerialNo(String cebSerialNo) {
        this.cebSerialNo = cebSerialNo;
    }

    public Double getMaximumOutput() {
        return maximumOutput;
    }

    public void setMaximumOutput(Double maximumOutput) {
        this.maximumOutput = maximumOutput;
    }

    public Integer getStationId() {
        return stationId;
    }

    public void setStationId(Integer stationId) {
        this.stationId = stationId;
    }

    public String getChargePointModel() {
        return chargePointModel;
    }

    public void setChargePointModel(String chargePointModel) {
        this.chargePointModel = chargePointModel;
    }

    public String getChargePointVendor() {
        return chargePointVendor;
    }

    public void setChargePointVendor(String chargePointVendor) {
        this.chargePointVendor = chargePointVendor;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getQrCodeData() {
        return qrCodeData;
    }

    public void setQrCodeData(String qrCodeData) {
        this.qrCodeData = qrCodeData;
    }

    public LocalDateTime getQrCodeGeneratedAt() {
        return qrCodeGeneratedAt;
    }

    public void setQrCodeGeneratedAt(LocalDateTime qrCodeGeneratedAt) {
        this.qrCodeGeneratedAt = qrCodeGeneratedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Boolean getRegistrationSuccess() {
        return registrationSuccess;
    }

    public void setRegistrationSuccess(Boolean registrationSuccess) {
        this.registrationSuccess = registrationSuccess;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

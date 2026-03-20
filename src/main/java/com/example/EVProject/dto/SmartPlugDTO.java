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

    private String idDevice;
    private String cebSerialNo;
    private Double maximumOutput;
    private Integer stationId;

    // QR Code Information
    private String qrCodeData;
    private LocalDateTime qrCodeGeneratedAt;

    public String getDeviceId() {
        return idDevice;
    }

    public void setDeviceId(String deviceId) {
        this.idDevice = deviceId;
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
}


package com.example.EVProject.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "smart_plug")
public class SmartPlug {

    @Id
    @Column(name = "id_device")
    private String idDevice;

    @Column(name = "ceb_serial_no")
    private String cebSerialNo;

    @Column(name = "maximum_output")
    private Double maximumOutput;

    @Column(name = "station_id")
    private Integer stationId;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "charge_point_model")
    private String chargePointModel;

    @Column(name = "charge_point_vendor")
    private String chargePointVendor;

    @Column(name = "firmware_version")
    private String firmwareVersion;

    @Column(name = "qr_code_data")
    private String qrCodeData;

    @Column(name = "qr_code_generated_at")
    private LocalDateTime qrCodeGeneratedAt;

    @Column(name = "status")
    private String status;

    @ManyToOne
    @JoinColumn(name = "station_id", referencedColumnName = "station_id", insertable = false, updatable = false)
    private ChargingStation chargingStation;

    // getters and setters

    public SmartPlug getStation(SmartPlug station) {
        return station;
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


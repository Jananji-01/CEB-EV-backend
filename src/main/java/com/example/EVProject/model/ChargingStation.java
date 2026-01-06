package com.example.EVProject.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "charging_station")
public class ChargingStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "station_id")
    private Integer stationId;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "solar_power_available")
    private Double solarPowerAvailable;

    @Column(name = "status")
    private String status;

    @Column(name = "error_code")
    private String errorCode;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "solar_owner_id")
    private Integer solarOwnerId;

    @Column(name = "id_device")
    private String idDevice;


    @ManyToOne
    @JoinColumn(name = "solar_owner_id", referencedColumnName = "solar_owner_id", insertable = false, updatable = false)
    private RooftopSolarOwner solarOwner;

    @ManyToOne
    @JoinColumn(name = "id_device",referencedColumnName = "id_device", insertable = false,updatable = false)
    private SmartPlug smartPlug;
    // getters and setters

}
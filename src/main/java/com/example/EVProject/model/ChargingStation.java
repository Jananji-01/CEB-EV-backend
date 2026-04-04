package com.example.EVProject.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Data
@Entity
@Table(name = "CHARGING_STATION")
public class ChargingStation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "charging_station_seq_gen")
    @SequenceGenerator(
            name = "charging_station_seq_gen",
            sequenceName = "CHARGING_STATION_SEQ",
            allocationSize = 1
    )
    @Column(name = "STATION_ID")
    private Integer stationId;

    @Column(name = "LATITUDE")
    private Double latitude;

    @Column(name = "LONGITUDE")
    private Double longitude;

    @Column(name = "station_name")
    private String stationName;

    @Column(name = "SOLAR_POWER_AVAILABLE")
    private Double solarPowerAvailable;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "ERROR_CODE")
    private String errorCode;

    @Column(name = "TIMESTAMP_COL", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "SOLAR_OWNER_ID")
    private Integer solarOwnerId;

    @Column(name = "ID_DEVICE")
    private String idDevice;

    @ManyToOne
    @JoinColumn(
            name = "SOLAR_OWNER_ID",
            referencedColumnName = "SOLAR_OWNER_ID",
            insertable = false,
            updatable = false
    )
    private RooftopSolarOwner solarOwner;

    @ManyToOne
    @JoinColumn(
            name = "ID_DEVICE",
            referencedColumnName = "ID_DEVICE",
            insertable = false,
            updatable = false
    )
    private SmartPlug smartPlug;
}
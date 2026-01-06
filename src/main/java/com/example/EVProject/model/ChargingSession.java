package com.example.EVProject.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "charging_sessions")
public class ChargingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Integer sessionId;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "soc")
    private Double soc;

    @Column(name = "charging_mode")
    private String chargingMode;

    @Column(name = "total_consumption")
    private Double totalConsumption;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "id_device")
    private String idDevice;

    @Transient
    private String status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_device", referencedColumnName = "id_device", insertable = false, updatable = false)
    private SmartPlug smartPlug;

    // getters and setters

}

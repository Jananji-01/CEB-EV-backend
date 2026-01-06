package com.example.EVProject.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "device_registry")
public class DeviceRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_device", unique = true, nullable = false)
    private String idDevice;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;
}

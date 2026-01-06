package com.example.EVProject.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "id_tag_info")
public class IdTagInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_tag", unique = true, nullable = false)
    private String idTag;

    @Column(name = "id_device", nullable = false)
    private String idDevice;

    @Column(name = "status", nullable = false)
    private String status; // Accepted, Invalid, Blocked, etc.

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}

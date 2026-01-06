package com.example.EVProject.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "ocpp_message_log")
public class OcppMessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_device", nullable = false)
    private String idDevice;

    @Column(name = "message_id", nullable = false)
    private String messageId;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "message_type_id", nullable = false)
    private Integer messageTypeId;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(columnDefinition = "TEXT")
    private String response;

    private LocalDateTime receivedAt;
    private LocalDateTime respondedAt;
}

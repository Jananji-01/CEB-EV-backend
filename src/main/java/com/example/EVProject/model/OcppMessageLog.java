//package com.example.EVProject.model;
//
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.Setter;
//import java.time.LocalDateTime;
//
//@Getter
//@Setter
//@Entity
//@Table(name = "ocpp_message_log")
//public class OcppMessageLog {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Integer id;
//
//    @Column(name = "id_device", nullable = false)
//    private String idDevice;
//
//    @Column(name = "message_id", nullable = false)
//    private String messageId;
//
//    @Column(name = "action", nullable = false)
//    private String action;
//
//    @Column(name = "message_type_id", nullable = false)
//    private Integer messageTypeId;
//
//    @Column(columnDefinition = "TEXT")
//    private String payload;
//
//    @Column(columnDefinition = "TEXT")
//    private String response;
//
//    private LocalDateTime receivedAt;
//    private LocalDateTime respondedAt;
//}

package com.example.EVProject.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Data
@Entity
@Table(name = "OCPP_MESSAGE_LOG")  // make sure table name matches Oracle
public class OcppMessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ocpp_log_seq_gen")
    @SequenceGenerator(
            name = "ocpp_log_seq_gen",
            sequenceName = "OCPP_MESSAGE_LOG_SEQ",
            allocationSize = 1
    )
    @Column(name = "ID")
    private Long id;   // Use Long instead of Integer for sequence

    @Column(name = "ID_DEVICE", nullable = false)
    private String idDevice;

    @Column(name = "MESSAGE_ID", nullable = false)
    private String messageId;

    @Column(name = "ACTION", nullable = false)
    private String action;

    @Column(name = "MESSAGE_TYPE_ID", nullable = false)
    private Integer messageTypeId;

    @Column(columnDefinition = "CLOB")
    private String payload;

    @Column(columnDefinition = "CLOB")
    private String response;

    @Column(name = "RECEIVED_AT")
    private LocalDateTime receivedAt;

    @Column(name = "RESPONDED_AT")
    private LocalDateTime respondedAt;
}
// OcppWebSocketSession.java
package com.example.EVProject.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "ocpp_websocket_session")
public class OcppWebSocketSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ocpp_ws_session_seq")
    @SequenceGenerator(name = "ocpp_ws_session_seq", sequenceName = "OCPP_WS_SESSION_SEQ", allocationSize = 1)
    private Integer id;
    
    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;
    
    @Column(name = "id_device", nullable = false)
    private String idDevice;
    
    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;
    
    @Column(name = "disconnected_at")
    private LocalDateTime disconnectedAt;
    
    @Column(name = "status")
    private String status; // CONNECTED, DISCONNECTED
    
    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;
}
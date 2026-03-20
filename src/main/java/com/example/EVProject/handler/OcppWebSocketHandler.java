package com.example.EVProject.handler;

import com.example.EVProject.repositories.SmartPlugRepository;
import com.example.EVProject.services.OcppWebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

@Component
public class OcppWebSocketHandler extends TextWebSocketHandler {
    
    @Autowired
    private OcppWebSocketService ocppWebSocketService;

    @Autowired
    private SmartPlugRepository smartPlugRepository;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Extract deviceId from URL path: /ws-ocpp/{deviceId}
        String path = session.getUri().getPath();
        String deviceId = extractDeviceIdFromPath(path);
        
        if (deviceId == null || deviceId.isEmpty()) {
            System.err.println("❌ No device ID in WebSocket URL");
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        // Check if device exists in database
        if (!smartPlugRepository.existsById(deviceId)) {
            System.err.println("❌ Device not registered: " + deviceId);
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Unknown device"));
            return;
        }
        
        // Store deviceId in session attributes
        session.getAttributes().put("deviceId", deviceId);
        
        // Handle connection
        ocppWebSocketService.handleOcppConnection(session, deviceId);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String deviceId = (String) session.getAttributes().get("deviceId");
        
        if (deviceId == null) {
            System.err.println("❌ Received message without device ID");
            return;
        }
        
        ocppWebSocketService.handleOcppMessage(session, message.getPayload());
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        ocppWebSocketService.handleOcppDisconnection(session);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("❌ WebSocket transport error: " + exception.getMessage());
        session.close(CloseStatus.SERVER_ERROR);
    }
    
    /**
     * Extract deviceId from URL path like /ws-ocpp/DEV-1001
     */
    private String extractDeviceIdFromPath(String path) {
        if (path == null) return null;
        
        String[] parts = path.split("/");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return null;
    }
}

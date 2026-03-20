package com.example.EVProject.services;

import com.example.EVProject.model.OcppWebSocketSession;
import com.example.EVProject.repositories.OcppWebSocketSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OcppWebSocketService {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private OcppWebSocketSessionRepository sessionRepository;
    
    @Autowired
    private OcppMessageProcessor ocppMessageProcessor;
    
    private final Map<String, WebSocketSession> deviceSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToDevice = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Handle new WebSocket connection
     */
    public void handleOcppConnection(WebSocketSession session, String deviceId) {
        System.out.println("🔌 Device connected via OCPP WebSocket: " + deviceId);
        
        // Store session mappings
        deviceSessions.put(deviceId, session);
        sessionToDevice.put(session.getId(), deviceId);
        
        // Save to database
        OcppWebSocketSession sessionEntity = new OcppWebSocketSession();
        sessionEntity.setSessionId(session.getId());
        sessionEntity.setIdDevice(deviceId);
        sessionEntity.setConnectedAt(LocalDateTime.now());
        sessionEntity.setStatus("CONNECTED");
        sessionRepository.save(sessionEntity);
        
        // Notify frontend
        notifyDeviceConnected(deviceId, true);
    }
    
    /**
     * Handle incoming WebSocket message
     */
    public void handleOcppMessage(WebSocketSession session, String message) {
        String sessionId = session.getId();
        String deviceId = sessionToDevice.get(sessionId);
        
        if (deviceId == null) {
            System.err.println("❌ Received message from unknown session");
            return;
        }
        
        try {
            // Process OCPP message
            String response = ocppMessageProcessor.processMessage(deviceId, message);
            
            // Send response back to device
            if (response != null && session.isOpen()) {
                session.sendMessage(new TextMessage(response));
            }
            
        } catch (IOException e) {
            System.err.println("❌ Failed to send response to device: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error processing OCPP message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handle WebSocket disconnection
     */
    public void handleOcppDisconnection(WebSocketSession session) {
        String deviceId = sessionToDevice.remove(session.getId());
        
        if (deviceId != null) {
            deviceSessions.remove(deviceId);
            System.out.println("🔌 Device disconnected: " + deviceId);
            
            // Update database
            sessionRepository.findBySessionId(session.getId()).ifPresent(sessionEntity -> {
                sessionEntity.setDisconnectedAt(LocalDateTime.now());
                sessionEntity.setStatus("DISCONNECTED");
                sessionRepository.save(sessionEntity);
            });
            
            // Notify frontend
            notifyDeviceConnected(deviceId, false);
        }
    }
    
    /**
     * Send RemoteStartTransaction to device
     */
    public boolean sendRemoteStartTransaction(String deviceId, String idTag, int connectorId) {
        WebSocketSession session = deviceSessions.get(deviceId);
        
        if (session == null || !session.isOpen()) {
            System.err.println("❌ Cannot send RemoteStartTransaction: Device not connected");
            return false;
        }
        
        try {
            // Create OCPP RemoteStartTransaction message
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("idTag", idTag);
            payload.put("connectorId", connectorId);
            
            String messageId = "remote-start-" + System.currentTimeMillis();
            
            ArrayNode message = objectMapper.createArrayNode();
            message.add(2); // MessageTypeId for CALL
            message.add(messageId);
            message.add("RemoteStartTransaction");
            message.add(payload);
            
            String messageJson = message.toString();
            session.sendMessage(new TextMessage(messageJson));
            
            System.out.println("✅ Sent RemoteStartTransaction to " + deviceId);
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Failed to send RemoteStartTransaction: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Send RemoteStopTransaction to device
     */
    public boolean sendRemoteStopTransaction(String deviceId, int transactionId) {
        WebSocketSession session = deviceSessions.get(deviceId);
        
        if (session == null || !session.isOpen()) {
            System.err.println("❌ Cannot send RemoteStopTransaction: Device not connected");
            return false;
        }
        
        try {
            // Create OCPP RemoteStopTransaction message
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("transactionId", transactionId);
            
            String messageId = "remote-stop-" + System.currentTimeMillis();
            
            ArrayNode message = objectMapper.createArrayNode();
            message.add(2); // MessageTypeId for CALL
            message.add(messageId);
            message.add("RemoteStopTransaction");
            message.add(payload);
            
            String messageJson = message.toString();
            session.sendMessage(new TextMessage(messageJson));
            
            System.out.println("✅ Sent RemoteStopTransaction to " + deviceId);
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Failed to send RemoteStopTransaction: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if device is connected
     */
    public boolean isDeviceConnected(String deviceId) {
        WebSocketSession session = deviceSessions.get(deviceId);
        return session != null && session.isOpen();
    }
    
    /**
     * Notify frontend via STOMP
     */
    private void notifyDeviceConnected(String deviceId, boolean connected) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("type", "DEVICE_CONNECTION");
        message.put("idDevice", deviceId);
        message.put("connected", connected);
        message.put("timestamp", LocalDateTime.now().toString());
        
        messagingTemplate.convertAndSend("/topic/device/" + deviceId, message);
        messagingTemplate.convertAndSend("/topic/charging", message);
    }

    public Double getDeviceEnergy(String deviceId) {
        // Simple implementation - returns 0.0
        // You can implement proper logic later
        return 0.0;
    }

}
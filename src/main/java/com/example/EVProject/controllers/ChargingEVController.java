package com.example.EVProject.controllers;

import com.example.EVProject.dto.ChargingSessionDTO;
import com.example.EVProject.model.ChargingSession;
import com.example.EVProject.model.EvOwner;
import com.example.EVProject.model.IdTagInfo;
import com.example.EVProject.services.ChargingSessionService;
import com.example.EVProject.services.OcppWebSocketService;
import com.example.EVProject.repositories.SmartPlugRepository;
import com.example.EVProject.repositories.IdTagInfoRepository;
import com.example.EVProject.repositories.ChargingSessionRepository;
import com.example.EVProject.repositories.EvOwnerRepository;
import com.example.EVProject.model.SmartPlug;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/charging")
public class ChargingEVController {
    
    @Autowired
    private OcppWebSocketService ocppWebSocketService;
    
    @Autowired
    private ChargingSessionService chargingSessionService;
    
    @Autowired
    private SmartPlugRepository smartPlugRepository;
    
    @Autowired
    private IdTagInfoRepository idTagInfoRepository;

    @Autowired
    private EvOwnerRepository evOwnerRepository;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChargingSessionRepository chargingSessionRepository;
    
    // Check device connection status
    @GetMapping("/device/{idDevice}/status")
    public ResponseEntity<Map<String, Object>> getDeviceStatus(@PathVariable String idDevice) {
        boolean isConnected = ocppWebSocketService.isDeviceConnected(idDevice);
        
        Map<String, Object> response = new HashMap<>();
        response.put("idDevice", idDevice);
        response.put("connected", isConnected);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("message", isConnected ? "Device is connected via WebSocket" : "Device is not connected");
        
        return ResponseEntity.ok(response);
    }
    
    // Get device details for frontend display
    @GetMapping("/device/{idDevice}/details")
    public ResponseEntity<Map<String, Object>> getDeviceDetails(@PathVariable String idDevice) {
        try {
            Optional<SmartPlug> plugOpt = smartPlugRepository.findById(idDevice);
            Map<String, Object> details = new HashMap<>();
            
            if (plugOpt.isEmpty()) {
                // Device not in database, return basic info
                details.put("idDevice", idDevice);
                details.put("registered", false);
                details.put("message", "Device not registered in database. Attempting WebSocket connection.");
                details.put("connected", ocppWebSocketService.isDeviceConnected(idDevice));
                details.put("isCharging", false);
                
                return ResponseEntity.ok(details);
            }
            
            // Device found in database
            SmartPlug plug = plugOpt.get();
            details.put("idDevice", plug.getIdDevice());
            details.put("cebSerialNo", plug.getCebSerialNo() != null ? plug.getCebSerialNo() : "N/A");
            details.put("maximumOutput", plug.getMaximumOutput() != null ? plug.getMaximumOutput() : 7.4);
            details.put("chargePointModel", plug.getChargePointModel() != null ? plug.getChargePointModel() : "Unknown");
            details.put("chargePointVendor", plug.getChargePointVendor() != null ? plug.getChargePointVendor() : "Unknown");
            details.put("connected", ocppWebSocketService.isDeviceConnected(idDevice));
            details.put("registered", true);
            details.put("stationId", plug.getStationId());
            details.put("firmwareVersion", plug.getFirmwareVersion() != null ? plug.getFirmwareVersion() : "N/A");
            
            // Check for active session
            ChargingSession activeSession = chargingSessionService.getActiveSession(idDevice);
            if (activeSession != null) {
                details.put("activeSession", convertSessionToMap(activeSession));
                details.put("isCharging", true);
            } else {
                details.put("isCharging", false);
            }
            
            return ResponseEntity.ok(details);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("idDevice", idDevice);
            error.put("error", e.getMessage());
            error.put("connected", false);
            error.put("isCharging", false);
            error.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.status(500).body(error);
        }
    }
    
    // Get all active charging sessions
    @GetMapping("/active-sessions")
    public ResponseEntity<?> getActiveSessions() {
        try {
            return ResponseEntity.ok(chargingSessionService.getActiveSessions());
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch active sessions");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // @PostMapping("/device/{idDevice}/prepare")
    // public ResponseEntity<Map<String, Object>> prepareDevice(
    //         @PathVariable String idDevice,
    //         @RequestBody(required = false) Map<String, String> request) {
    //     try {
    //         // Extract EV owner account number from request
    //         String evOwnerAccountNo = null;
    //         if (request != null && request.containsKey("evOwnerAccountNo")) {
    //             evOwnerAccountNo = request.get("evOwnerAccountNo");
    //         }

    //         if (evOwnerAccountNo == null || evOwnerAccountNo.isEmpty()) {
    //             return ResponseEntity.badRequest().body(Map.of(
    //                 "success", false,
    //                 "message", "EV owner account number is required"
    //             ));
    //         }

    //         // Look up owner by account number
    //         Optional<EvOwner> ownerOpt = evOwnerRepository.findByEAccountNumber(evOwnerAccountNo);
    //         if (ownerOpt.isEmpty()) {
    //             return ResponseEntity.badRequest().body(Map.of(
    //                 "success", false,
    //                 "message", "Invalid EV owner account number"
    //             ));
    //         }
    //         EvOwner owner = ownerOpt.get();
    //         String idTag = owner.getIdTag();

    //         // Create/update IdTagInfo record linking device to owner's persistent idTag
    //         IdTagInfo tagInfo = new IdTagInfo();
    //         tagInfo.setIdDevice(idDevice);
    //         tagInfo.setIdTag(idTag);
    //         tagInfo.setStatus("Accepted");
    //         tagInfo.setExpiryDate(LocalDateTime.now(ZoneOffset.UTC).plusYears(1)); // 1 year validity
    //         tagInfo.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
    //         idTagInfoRepository.save(tagInfo);
    //         System.out.println("💾 Linked device " + idDevice + " to owner " + evOwnerAccountNo + " (idTag: " + idTag + ")");

    //         // Send RemoteStartTransaction with the owner's idTag
    //         boolean sent = ocppWebSocketService.sendRemoteStartTransaction(idDevice, idTag, 1);
    //         if (!sent) {
    //             return ResponseEntity.badRequest().body(Map.of(
    //                 "success", false,
    //                 "message", "Device not connected or failed to send prepare command"
    //             ));
    //         }
    //         return ResponseEntity.ok(Map.of(
    //             "success", true,
    //             "message", "Prepare command sent to device. Device is now ready."
    //         ));
    //     } catch (Exception e) {
    //         return ResponseEntity.internalServerError().body(Map.of(
    //             "success", false,
    //             "message", "Failed to prepare device: " + e.getMessage()
    //         ));
    //     }
    // }

    @PostMapping("/device/{idDevice}/prepare")
    public ResponseEntity<Map<String, Object>> prepareDevice(
            @PathVariable String idDevice,
            @RequestBody(required = false) Map<String, String> request) {
        try {
            String evOwnerAccountNo = request.get("evOwnerAccountNo");
            
            if (evOwnerAccountNo == null || evOwnerAccountNo.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "EV owner account number is required"
                ));
            }

            Optional<EvOwner> ownerOpt = evOwnerRepository.findByEAccountNumber(evOwnerAccountNo);
            if (ownerOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid EV owner account number"
                ));
            }
            
            EvOwner owner = ownerOpt.get();
            String idTag = owner.getIdTag();

            LocalDateTime now = LocalDateTime.now();
            Optional<IdTagInfo> existingTag = idTagInfoRepository.findByIdTagAndIdDevice(idTag, idDevice);

            IdTagInfo tagInfo;
            if (existingTag.isPresent()) {
                tagInfo = existingTag.get();
                // If expired, update status; otherwise just extend expiry
                if (tagInfo.getExpiryDate().isBefore(now)) {
                    tagInfo.setStatus("Accepted");
                    tagInfo.setExpiryDate(now.plusHours(6));
                } else {
                    // Still valid – optionally extend to keep it fresh
                    tagInfo.setExpiryDate(now.plusHours(6));
                }
            } else {
                tagInfo = new IdTagInfo();
                tagInfo.setIdDevice(idDevice);
                tagInfo.setIdTag(idTag);
                tagInfo.setStatus("Accepted");
                tagInfo.setExpiryDate(now.plusHours(6));
                tagInfo.setCreatedAt(now);
            }
            
            // Generate idTag if null
            if (idTag == null || idTag.trim().isEmpty()) {
                idTag = generateIdTag(idDevice);
                owner.setIdTag(idTag);
                evOwnerRepository.save(owner);
            }
            
            // ✅ ALWAYS CREATE NEW RECORD - Keep history
            IdTagInfo tagInfo = new IdTagInfo();
            tagInfo.setIdDevice(idDevice);
            tagInfo.setIdTag(idTag);
            tagInfo.setStatus("Accepted");
            tagInfo.setExpiryDate(LocalDateTime.now(ZoneOffset.UTC).plusYears(1));
            tagInfo.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            idTagInfoRepository.save(tagInfo);
            
            System.out.println("✅ Created new authorization record for device " + idDevice + 
                            " with idTag " + idTag + " at " + tagInfo.getCreatedAt());

            // Send RemoteStartTransaction to device
            boolean sent = ocppWebSocketService.sendRemoteStartTransaction(idDevice, idTag, 1);
            if (!sent) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Device not connected or failed to send prepare command"
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Device is ready for charging",
                "idTag", idTag,
                "authorizationId", tagInfo.getId()
            ));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to prepare device: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/device/{idDevice}/start")
    public ResponseEntity<Map<String, Object>> startCharging(
            @PathVariable String idDevice,
            @RequestBody Map<String, String> request) {
        
        try {
            // Check if device is connected via WebSocket
            boolean isConnected = ocppWebSocketService.isDeviceConnected(idDevice);
            if (!isConnected) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Device is not connected via WebSocket",
                    "idDevice", idDevice
                ));
            }

            String evOwnerAccountNo = request.get("evOwnerAccountNo");
            if (evOwnerAccountNo == null || evOwnerAccountNo.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "EV owner account number is required"
                ));
            }

            // Look up owner by account number
            Optional<EvOwner> ownerOpt = evOwnerRepository.findByEAccountNumber(evOwnerAccountNo);
            if (ownerOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid EV owner account number"
                ));
            }
            EvOwner owner = ownerOpt.get();
            String idTag = owner.getIdTag();

            // Create/update IdTagInfo record (ensures association exists)
            IdTagInfo tagInfo = new IdTagInfo();
            tagInfo.setIdDevice(idDevice);
            tagInfo.setIdTag(idTag);
            tagInfo.setStatus("Accepted");
            tagInfo.setExpiryDate(LocalDateTime.now(ZoneOffset.UTC).plusYears(1));
            tagInfo.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            idTagInfoRepository.save(tagInfo);
            
            int connectorId = Integer.parseInt(request.getOrDefault("connectorId", "1"));
            
            // Send RemoteStartTransaction with owner's idTag
            boolean sent = ocppWebSocketService.sendRemoteStartTransaction(idDevice, idTag, connectorId);
            if (!sent) {
                return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to send start command to device"
                ));
            }
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Start command sent to device");
            response.put("idDevice", idDevice);
            response.put("idTag", idTag);
            response.put("connectorId", connectorId);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("webSocket", true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to start charging: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/device/{idDevice}/stop")
    public ResponseEntity<Map<String, Object>> stopCharging(
            @PathVariable String idDevice,
            @RequestBody(required = false) Map<String, String> request) {
        
        System.out.println("=== SMART STOP: Stop Charging Called ===");
        System.out.println("Device: " + idDevice);
        
        try {
            // Method 1: Check if transactionId is provided in request
            Integer transactionId = null;
            
            if (request != null && request.containsKey("transactionId")) {
                String transactionIdStr = request.get("transactionId");
                System.out.println("SMART STOP: TransactionId from request: " + transactionIdStr);
                
                try {
                    transactionId = Integer.parseInt(transactionIdStr);
                    System.out.println("SMART STOP: Using provided transactionId: " + transactionId);
                } catch (NumberFormatException e) {
                    System.err.println("SMART STOP: Invalid transactionId format, will try to find active session");
                }
            }
            
            // Method 2: If no transactionId or invalid, find active session for device
            ChargingSession activeSession = null;
            if (transactionId == null) {
                System.out.println("SMART STOP: Looking for active session for device: " + idDevice);
                activeSession = chargingSessionService.getActiveSession(idDevice);
                
                if (activeSession != null) {
                    transactionId = activeSession.getSessionId();
                    System.out.println("SMART STOP: Found active session with ID: " + transactionId);
                } else {
                    System.out.println("SMART STOP: No active session found");
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "No active charging session found for this device",
                        "idDevice", idDevice
                    ));
                }
            } else {
                // Get the session by transactionId to use later for energy calculation
                Optional<ChargingSession> sessionOpt = chargingSessionRepository.findById(transactionId);
                if (sessionOpt.isPresent()) {
                    activeSession = sessionOpt.get();
                }
            }
            
            // Method 3: Verify the session exists and belongs to this device
            if (transactionId != null) {
                System.out.println("SMART STOP: Final transactionId to stop: " + transactionId);
                
                // Verify session exists and belongs to this device
                Optional<ChargingSession> sessionOpt = chargingSessionRepository.findById(transactionId);
                if (sessionOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Transaction ID not found: " + transactionId,
                        "idDevice", idDevice
                    ));
                }
                
                ChargingSession session = sessionOpt.get();
                if (!idDevice.equals(session.getIdDevice())) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Transaction ID does not belong to this device",
                        "idDevice", idDevice,
                        "transactionId", transactionId
                    ));
                }
                
                // Store session for energy calculation
                activeSession = session;
            }
            
            String timestamp = request != null ? 
                request.getOrDefault("timestamp", LocalDateTime.now().toString()) : 
                LocalDateTime.now().toString();
            
            // Send stop command via WebSocket
            boolean sent = ocppWebSocketService.sendRemoteStopTransaction(idDevice, transactionId);
            
            if (!sent) {
                System.out.println("SMART STOP: Failed to send WebSocket stop command, but will update session anyway");
            }
            
            // Update the session in database with proper energy calculation
            try {
                // Calculate energy from session total consumption if available
                Long energy = 0L;
                if (activeSession != null && activeSession.getTotalConsumption() != null) {
                    // If session already has total consumption, use it
                    energy = activeSession.getTotalConsumption().longValue();
                    System.out.println("SMART STOP: Using existing session energy: " + energy + " kWh");
                } else if (request != null && request.containsKey("meterStop")) {
                    // If request has meterStop value, use it
                    try {
                        energy = Long.parseLong(request.get("meterStop"));
                        System.out.println("SMART STOP: Using meterStop from request: " + energy + " kWh");
                    } catch (NumberFormatException e) {
                        System.err.println("SMART STOP: Invalid meterStop format in request");
                    }
                } else {
                    // Default energy value (for simulation/testing)
                    energy = 15L; // Default 15 kWh for simulation
                    System.out.println("SMART STOP: Using default energy: " + energy + " kWh");
                }
                
                // Update session end time and energy
                chargingSessionService.endChargingSession(transactionId, energy, timestamp);
                
                System.out.println("SMART STOP: Session updated in database with energy: " + energy + " kWh");
            } catch (Exception e) {
                System.err.println("SMART STOP: Could not update session in DB: " + e.getMessage());
                // Continue anyway - the session might have been updated by WebSocket handler
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Charging stopped successfully");
            response.put("idDevice", idDevice);
            response.put("transactionId", transactionId);
            response.put("timestamp", timestamp);
            response.put("autoDetected", request == null || !request.containsKey("transactionId"));
            
            // Add energy information if available
            if (activeSession != null && activeSession.getTotalConsumption() != null) {
                response.put("totalConsumption", activeSession.getTotalConsumption());
                response.put("estimatedCost", String.format("$%.2f", activeSession.getTotalConsumption() * 0.15));
            }
            
            System.out.println("SMART STOP: Success response: " + response);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("SMART STOP: Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to stop charging: " + e.getMessage(),
                "error", e.getClass().getSimpleName()
            ));
        }
    }
    
    // Get device connection status
    @GetMapping("/device/{idDevice}/connection")
    public ResponseEntity<Map<String, Object>> getConnectionStatus(@PathVariable String idDevice) {
        Map<String, Object> status = new HashMap<>();
        status.put("idDevice", idDevice);
        status.put("connected", ocppWebSocketService.isDeviceConnected(idDevice));
        status.put("timestamp", LocalDateTime.now().toString());
        status.put("message", ocppWebSocketService.isDeviceConnected(idDevice) ? 
            "Device is connected and ready for charging" : 
            "Device is not connected. Please check power and network connection.");
        
        return ResponseEntity.ok(status);
    }
    
    // Send heartbeat to device
    @PostMapping("/device/{idDevice}/heartbeat")
    public ResponseEntity<Map<String, Object>> sendHeartbeat(@PathVariable String idDevice) {
        try {
            // This is a placeholder for sending heartbeat via WebSocket
            // In real implementation, heartbeat is usually initiated by the device
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Heartbeat check performed");
            response.put("idDevice", idDevice);
            response.put("connected", ocppWebSocketService.isDeviceConnected(idDevice));
            response.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to send heartbeat: " + e.getMessage()
            ));
        }
    }
    
    // Get charging session by ID
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getChargingSession(@PathVariable Integer sessionId) {
        try {
            ChargingSessionDTO session = chargingSessionService.getSessionById(sessionId);
            if (session == null) {
                return ResponseEntity.status(404).body(Map.of(
                    "error", "Session not found",
                    "sessionId", sessionId
                ));
            }
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to fetch session",
                "message", e.getMessage()
            ));
        }
    }
    
    // WebSocket message endpoints for real-time communication
    @MessageMapping("/charging/start")
    @SendTo("/topic/charging/status")
    public Map<String, Object> handleStartMessage(Map<String, String> message) {
        String idDevice = message.get("idDevice");
        String idTag = message.get("idTag");
        String timestamp = message.getOrDefault("timestamp", LocalDateTime.now().toString());
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "CHARGING_STARTED");
        response.put("idDevice", idDevice);
        response.put("idTag", idTag);
        response.put("timestamp", timestamp);
        response.put("serverTime", LocalDateTime.now().toString());
        response.put("message", "Charging session initiated");
        
        return response;
    }
    
    @MessageMapping("/charging/stop")
    @SendTo("/topic/charging/status")
    public Map<String, Object> handleStopMessage(Map<String, String> message) {
        String idDevice = message.get("idDevice");
        String transactionId = message.get("transactionId");
        String timestamp = message.getOrDefault("timestamp", LocalDateTime.now().toString());
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "CHARGING_STOPPED");
        response.put("idDevice", idDevice);
        response.put("transactionId", transactionId);
        response.put("timestamp", timestamp);
        response.put("serverTime", LocalDateTime.now().toString());
        response.put("message", "Charging session terminated");
        
        return response;
    }
    
    @MessageMapping("/charging/status")
    @SendTo("/topic/charging/updates")
    public Map<String, Object> handleStatusUpdate(Map<String, Object> message) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "STATUS_UPDATE");
        response.put("data", message);
        response.put("serverTime", LocalDateTime.now().toString());
        
        return response;
    }
    
    @SubscribeMapping("/device/{idDevice}/updates")
    public Map<String, Object> subscribeToDevice(@PathVariable String idDevice) {
        Map<String, Object> subscription = new HashMap<>();
        subscription.put("type", "SUBSCRIBED");
        subscription.put("idDevice", idDevice);
        subscription.put("timestamp", LocalDateTime.now().toString());
        subscription.put("message", "Subscribed to device updates");
        subscription.put("topics", new String[] {
            "/topic/device/" + idDevice,
            "/topic/charging/status",
            "/topic/charging/updates"
        });
        
        return subscription;
    }
    
    // Helper method to convert ChargingSession to Map
    private Map<String, Object> convertSessionToMap(ChargingSession session) {
        Map<String, Object> sessionMap = new HashMap<>();
        if (session == null) return sessionMap;
        
        sessionMap.put("sessionId", session.getSessionId());
        sessionMap.put("startTime", session.getStartTime());
        sessionMap.put("endTime", session.getEndTime());
        sessionMap.put("idDevice", session.getIdDevice());
        sessionMap.put("totalConsumption", session.getTotalConsumption());
        sessionMap.put("chargingMode", session.getChargingMode());
        sessionMap.put("soc", session.getSoc());
        sessionMap.put("amount", session.getAmount());
        sessionMap.put("status", session.getEndTime() == null ? "ACTIVE" : "COMPLETED");
        sessionMap.put("duration", calculateDuration(session.getStartTime(), session.getEndTime()));
        
        return sessionMap;
    }
    
    // Helper method to calculate duration
    private String calculateDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null) return "00:00:00";
        
        LocalDateTime endTime = end != null ? end : LocalDateTime.now();
        long seconds = java.time.Duration.between(start, endTime).getSeconds();
        
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
    
    // Generate unique idTag for device
    private String generateIdTag(String idDevice) {
        try {
            // Generate a simple ID tag based on device ID and timestamp
            String timestamp = String.valueOf(System.currentTimeMillis());
            String hash = idDevice + "-" + timestamp.substring(timestamp.length() - 6);
            return "IDT-" + hash.toUpperCase();
        } catch (Exception e) {
            return "IDT-" + idDevice + "-" + LocalDateTime.now().getSecond();
        }
    }
    
    // Test endpoint to check controller is working
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "ChargingEVController");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("endpoints", new String[] {
            "GET /api/charging/device/{idDevice}/details",
            "GET /api/charging/device/{idDevice}/status",
            "POST /api/charging/device/{idDevice}/start",
            "POST /api/charging/device/{idDevice}/stop",
            "GET /api/charging/active-sessions",
            "GET /api/charging/session/{sessionId}"
        });
        
        return ResponseEntity.ok(response);
    }
    
    // Emergency stop endpoint
    @PostMapping("/device/{idDevice}/emergency-stop")
    public ResponseEntity<Map<String, Object>> emergencyStop(@PathVariable String idDevice) {
        try {
            // Find active session for this device
            ChargingSession activeSession = chargingSessionService.getActiveSession(idDevice);
            
            Map<String, Object> response = new HashMap<>();
            
            if (activeSession != null) {
                // Stop the session
                chargingSessionService.endChargingSession(
                    activeSession.getSessionId(), 
                    activeSession.getTotalConsumption() != null ? activeSession.getTotalConsumption().longValue() : 0L,
                    LocalDateTime.now().toString()
                );
                
                // Send stop command via WebSocket
                ocppWebSocketService.sendRemoteStopTransaction(idDevice, activeSession.getSessionId());
                
                response.put("success", true);
                response.put("message", "Emergency stop executed");
                response.put("sessionId", activeSession.getSessionId());
                response.put("action", "SESSION_STOPPED");
            } else {
                response.put("success", true);
                response.put("message", "No active session found for emergency stop");
                response.put("action", "NO_ACTION");
            }
            
            response.put("idDevice", idDevice);
            response.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Emergency stop failed: " + e.getMessage(),
                "idDevice", idDevice
            ));
        }
    }
}
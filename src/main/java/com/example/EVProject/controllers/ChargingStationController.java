package com.example.EVProject.controllers;

import com.example.EVProject.dto.ChargingStationDTO;
import com.example.EVProject.dto.ChargingStationStatusUpdate;
import com.example.EVProject.dto.ChargingSessionDTO;
import com.example.EVProject.dto.MeterValueRequest;
import com.example.EVProject.model.IdTagInfo;
import com.example.EVProject.model.OcppMessageLog;
import com.example.EVProject.model.SmartPlug;
import com.example.EVProject.model.EvOwner;
import com.example.EVProject.repositories.IdTagInfoRepository;
import com.example.EVProject.repositories.OcppMessageLogRepository;
import com.example.EVProject.repositories.SmartPlugRepository;
import com.example.EVProject.repositories.EvOwnerRepository;
import com.example.EVProject.services.ChargingSessionService;
import com.example.EVProject.services.ChargingStationService;
import com.example.EVProject.services.MeterValueService;
import com.example.EVProject.services.BillingService;
import com.example.EVProject.services.OcppActionService;
import com.example.EVProject.utils.IdDeviceValidator;
import com.example.EVProject.utils.OcppMessageParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.Random;
import java.util.ArrayList;


@RestController
@RequestMapping("/api/charging-stations")
public class ChargingStationController {

    @Autowired
    private ChargingStationService service;

    @Autowired
    private IdDeviceValidator idDeviceValidator;

    @Autowired
    private OcppMessageLogRepository messageLogRepo;

    @Autowired
    private SmartPlugRepository smartPlugRepository;

    @Autowired
    private IdTagInfoRepository idTagInfoRepository;

    @Autowired
    private ChargingSessionService chargingSessionService;

    @Autowired
    private MeterValueService meterValueService;

    @Autowired
    private BillingService billingService;

    @Autowired
    private EvOwnerRepository evOwnerRepository;

    @Autowired
    private OcppActionService ocppActionService;

    @GetMapping
    public List<ChargingStationDTO> getAllStations() {
        return service.getAllStations();
    }

    @GetMapping("/{id}")
    public ChargingStationDTO getStationById(@PathVariable Integer id) {
        return service.getStationById(id);
    }

    @PostMapping
    public ChargingStationDTO saveStation(@RequestBody ChargingStationDTO dto) {
        return service.saveStation(dto);
    }

    @DeleteMapping("/{id}")
    public void deleteStation(@PathVariable Integer id) {
        service.deleteStation(id);
    }

    @PostMapping("/status")
    public ResponseEntity<?> updateChargingStationStatus(
            @RequestBody String rawBody,
            @RequestHeader("USER") String user,
            @RequestHeader("DIGEST") String digest,
            @RequestHeader("IdDevice") String idDevice) {

        LocalDateTime receivedTime = LocalDateTime.now();
        OcppMessageLog logEntry = new OcppMessageLog();

        try {
            if (!"test".equals(user) || !"test123".equals(digest)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "AUTHENTICATION_FAILURE"));
            }
            idDeviceValidator.validate(idDevice);
            var parsed = OcppMessageParser.parse(rawBody);
            if (!"StatusNotification".equalsIgnoreCase(parsed.action())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid action type"));
            }
            var payload = parsed.payload();
            Integer connectorId = payload.get("connectorId").asInt();
            String errorCode = payload.get("errorCode").asText();
            String status = payload.get("status").asText();
            String timestampStr = payload.get("timestamp").asText();
            LocalDateTime timestamp = LocalDateTime.parse(timestampStr.replace("Z", ""));

            logEntry.setIdDevice(idDevice);
            logEntry.setMessageId(parsed.messageId());
            logEntry.setAction(parsed.action());
            logEntry.setMessageTypeId(parsed.messageTypeId());
            logEntry.setPayload(payload.toString());
            logEntry.setReceivedAt(receivedTime);

            Integer statusCode = mapStatusStringToCode(status);
            service.updateChargingStationStatus(connectorId, statusCode, errorCode, timestamp);

            Object[] ocppResponse = new Object[]{3, parsed.messageId(), new HashMap<>()};
            logEntry.setResponse(new ObjectMapper().writeValueAsString(ocppResponse));
            logEntry.setRespondedAt(LocalDateTime.now());
            messageLogRepo.save(logEntry);

            return ResponseEntity.ok(ocppResponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            logEntry.setResponse("{\"error\": \"" + e.getMessage() + "\"}");
            logEntry.setRespondedAt(LocalDateTime.now());
            messageLogRepo.save(logEntry);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "INTERNAL_SERVER_ERROR"));
        }
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<?> handleHeartbeat(
            @RequestBody String rawBody,
            @RequestHeader("IdDevice") String idDevice) {

        LocalDateTime receivedTime = LocalDateTime.now();
        try {
            idDeviceValidator.validate(idDevice);
            var parsed = OcppMessageParser.parse(rawBody);
            if (parsed.messageTypeId() != 2 || !"Heartbeat".equalsIgnoreCase(parsed.action())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid message type or action. Expected Heartbeat CALL"));
            }

            ObjectNode payloadNode = ocppActionService.handleHeartbeat();
            Map<String, Object> payload = Map.of("currentTime", payloadNode.get("currentTime").asText());
            Object[] ocppResponse = new Object[]{3, parsed.messageId(), payload};

            if (messageLogRepo != null) {
                OcppMessageLog log = new OcppMessageLog();
                log.setIdDevice(idDevice);
                log.setMessageId(parsed.messageId());
                log.setAction(parsed.action());
                log.setMessageTypeId(parsed.messageTypeId());
                log.setPayload(parsed.payload().toString());
                log.setResponse(new ObjectMapper().writeValueAsString(ocppResponse));
                log.setReceivedAt(receivedTime);
                log.setRespondedAt(LocalDateTime.now());
                messageLogRepo.save(log);
            }
            return ResponseEntity.ok(ocppResponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_SERVER_ERROR"));
        }
    }

    // @PostMapping("/authorize")
    // public ResponseEntity<?> handleAuthorize(
    //         @RequestBody String rawBody,
    //         @RequestHeader("IdDevice") String headerIdDevice) {

    //     LocalDateTime receivedTime = LocalDateTime.now();

    //     try {
    //         // Validate IdDevice using your util class
    //         idDeviceValidator.validate(headerIdDevice);

    //         // Parse OCPP-like message
    //         var parsed = OcppMessageParser.parse(rawBody);
    //         if (!"Authorize".equalsIgnoreCase(parsed.action())) {
    //             return ResponseEntity.status(HttpStatus.BAD_REQUEST)
    //                     .body(Map.of("error", "Invalid action type. Expected 'Authorize'"));
    //         }

    //         // Extract IdDevice from request payload
    //         String bodyIdDevice = parsed.payload().has("IdDevice")
    //                 ? parsed.payload().get("IdDevice").asText()
    //                 : null;

    //         if (bodyIdDevice == null || bodyIdDevice.isEmpty()) {
    //             return ResponseEntity.status(HttpStatus.BAD_REQUEST)
    //                     .body(Map.of("error", "Missing IdDevice in payload"));
    //         }

    //         // Compare header and body IdDevice (must match)
    //         if (!headerIdDevice.equals(bodyIdDevice)) {
    //             return ResponseEntity.status(HttpStatus.BAD_REQUEST)
    //                     .body(Map.of("error", "Header and payload IdDevice mismatch"));
    //         }

    //         // Validate the IdDevice in the payload (redundant but explicit)
    //         idDeviceValidator.validate(bodyIdDevice);

    //         // Check if IdTag already exists for this IdDevice
    //         List<IdTagInfo> existingTags = idTagInfoRepository.findByIdDevice(bodyIdDevice);
    //         IdTagInfo tagRecord;
    //         String idTag;
    //         LocalDateTime expiryDate;

    //         IdTagInfo validTag = null;

    //         for (IdTagInfo tag : existingTags) {
    //             if (tag.getExpiryDate().isAfter(LocalDateTime.now())) {
    //                 validTag = tag;
    //                 break;
    //             }
    //         }

    //         if (validTag != null) {
    //             // reuse valid tag
    //             tagRecord = validTag;
    //             idTag = tagRecord.getIdTag();
    //             expiryDate = tagRecord.getExpiryDate();
    //         } else {
    //             // Fetch SmartPlug details (for cebSerialNo or username)
    //             SmartPlug plug = smartPlugRepository.findById(bodyIdDevice)
    //                     .orElseThrow(() -> new IllegalArgumentException("IdDevice not found: " + bodyIdDevice));

    //             idDeviceValidator.validate(plug.getIdDevice());

    //             String accountReference = (plug.getCebSerialNo() != null)
    //                     ? plug.getCebSerialNo()
    //                     : plug.getIdDevice();

    //             idTag = generateIdTag(accountReference);
    //             expiryDate = LocalDateTime.now().plusHours(6);

    //             tagRecord = new IdTagInfo();
    //             tagRecord.setIdDevice(bodyIdDevice);
    //             tagRecord.setIdTag(idTag);
    //             tagRecord.setStatus("Accepted");
    //             tagRecord.setExpiryDate(expiryDate);
    //             idTagInfoRepository.save(tagRecord);
    //         }

    //         // Build OCPP response according to documentation
    //         Map<String, Object> idTagInfo = Map.of(
    //                 "status", "Accepted",
    //                 "expiryDate", expiryDate.toString() + "Z",
    //                 "IdTag", idTag
    //         );

    //         Map<String, Object> payload = Map.of("idTagInfo", idTagInfo);
    //         Object[] ocppResponse = new Object[]{
    //                 3,
    //                 parsed.messageId(),
    //                 payload
    //         };

    //         // Optional logging (for debugging/traceability)
    //         if (messageLogRepo != null) {
    //             OcppMessageLog log = new OcppMessageLog();
    //             log.setIdDevice(bodyIdDevice);
    //             log.setMessageId(parsed.messageId());
    //             log.setAction(parsed.action());
    //             log.setMessageTypeId(parsed.messageTypeId());
    //             log.setPayload(parsed.payload().toString());
    //             log.setResponse(new ObjectMapper().writeValueAsString(ocppResponse));
    //             log.setReceivedAt(receivedTime);
    //             log.setRespondedAt(LocalDateTime.now());
    //             messageLogRepo.save(log);
    //         }

    //         return ResponseEntity.ok(ocppResponse);

    //     } catch (IllegalArgumentException e) {
    //         Map<String, Object> idTagInfo = Map.of("status", "Invalid");
    //         Map<String, Object> payload = Map.of("idTagInfo", idTagInfo);
    //         Object[] ocppResponse = new Object[]{3, "AUTH-REQ-FAILED", payload};
    //         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ocppResponse);
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
    //                 .body(Map.of("error", "INTERNAL_SERVER_ERROR"));
    //     }
    // }


@PostMapping("/authorize")
public ResponseEntity<?> handleAuthorize(
        @RequestBody String rawBody,
        @RequestHeader("IdDevice") String headerIdDevice) {

    LocalDateTime receivedTime = LocalDateTime.now();

    try {
        // Parse OCPP message
        var parsed = OcppMessageParser.parse(rawBody);
        
        // Get ALL EV owners with idTag
        List<EvOwner> allEvOwners = evOwnerRepository.findAll();
        List<EvOwner> evOwnersWithIdTag = new ArrayList<>();
        for (EvOwner owner : allEvOwners) {
            if (owner.getIdTag() != null && !owner.getIdTag().isEmpty()) {
                evOwnersWithIdTag.add(owner);
            }
        }
        
        if (evOwnersWithIdTag.isEmpty()) {
            System.out.println("❌ No EV owner with valid ID_TAG found");
            Object[] errorResponse = new Object[]{
                    3,
                    parsed.messageId(),
                    Map.of("idTagInfo", Map.of("status", "Invalid"))
            };
            return ResponseEntity.ok(errorResponse);
        }
        
        // Get random EV owner
        Random random = new Random();
        EvOwner randomOwner = evOwnersWithIdTag.get(random.nextInt(evOwnersWithIdTag.size()));
        String idTag = randomOwner.getIdTag();
        
        System.out.println("✅ Selected EV Owner: " + randomOwner.getUsername() + " with ID_TAG: " + idTag);
        
        // ✅ CRITICAL: Check if idTag exists in ID_TAG_INFO, if not, create it
        Optional<IdTagInfo> existingTagOpt = idTagInfoRepository.findByIdTag(idTag);
        IdTagInfo tagInfo;
        LocalDateTime expiryDate;
        String status;
        
        if (existingTagOpt.isPresent()) {
            tagInfo = existingTagOpt.get();
            expiryDate = tagInfo.getExpiryDate();
            status = tagInfo.getStatus();
            System.out.println("✅ Found existing ID_TAG_INFO - Expiry: " + expiryDate + ", Status: " + status);
        } else {
            // ✅ Create new ID_TAG_INFO entry
            expiryDate = LocalDateTime.now().plusYears(1);
            status = "Accepted";
            
            tagInfo = new IdTagInfo();
            tagInfo.setIdTag(idTag);
            tagInfo.setIdDevice(headerIdDevice);  // Link to the device
            tagInfo.setStatus(status);
            tagInfo.setExpiryDate(expiryDate);
            tagInfo.setCreatedAt(LocalDateTime.now());
            idTagInfoRepository.save(tagInfo);
            
            System.out.println("✅ Created new ID_TAG_INFO for idTag: " + idTag);
        }
        
        // Build response
        Map<String, Object> idTagInfoResponse = new HashMap<>();
        idTagInfoResponse.put("status", status);
        idTagInfoResponse.put("IdTag", idTag);
        idTagInfoResponse.put("expiryDate", expiryDate.toString() + "Z");
        
        Object[] ocppResponse = new Object[]{
                3,
                parsed.messageId(),
                Map.of("idTagInfo", idTagInfoResponse)
        };
        
        System.out.println("✅ Returning response with ID_TAG: " + idTag);
        
        return ResponseEntity.ok(ocppResponse);
        
    } catch (Exception e) {
        e.printStackTrace();
        Object[] errorResponse = new Object[]{
                3,
                "AUTH-REQ-FAILED",
                Map.of("idTagInfo", Map.of("status", "Invalid"))
        };
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

    @PostMapping("/startTransaction")
    @Transactional
    public ResponseEntity<?> handleStartTransaction(
            @RequestBody String rawBody,
            @RequestHeader("IdDevice") String headerIdDevice) {

        LocalDateTime receivedAt = LocalDateTime.now();
        try {
            var parsed = OcppMessageParser.parse(rawBody);
            if (parsed.messageTypeId() != 2 || !"StartTransaction".equalsIgnoreCase(parsed.action())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid message type or action. Expected StartTransaction"));
            }

            ObjectNode responsePayload = ocppActionService.handleStartTransaction(headerIdDevice, parsed.messageId(), parsed.payload());
            Object[] ocppResponse = new Object[]{3, parsed.messageId(), responsePayload};

            OcppMessageLog log = new OcppMessageLog();
            log.setIdDevice(headerIdDevice);
            log.setMessageId(parsed.messageId());
            log.setAction(parsed.action());
            log.setMessageTypeId(parsed.messageTypeId());
            log.setPayload(parsed.payload().toString());
            log.setResponse(new ObjectMapper().writeValueAsString(ocppResponse));
            log.setReceivedAt(receivedAt);
            log.setRespondedAt(LocalDateTime.now());
            messageLogRepo.save(log);

            return ResponseEntity.ok(ocppResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_SERVER_ERROR", "message", e.getMessage()));
        }
    }

    private Integer mapStatusStringToCode(String status) {
        return switch (status) {
            case "Available" -> 1;
            case "Preparing" -> 2;
            case "Charging" -> 3;
            case "Finishing" -> 4;
            case "Unavailable" -> 5;
            default -> throw new IllegalArgumentException("Unknown status: " + status);
        };
    }

    private String generateIdTag(String baseValue) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((baseValue + System.currentTimeMillis()).getBytes());
            String hex = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            return "IDT-" + hex.substring(0, 8).toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("Error generating IdTag", e);
        }
    }

    // @PostMapping("/stopTransaction")
    // @Transactional
    // public ResponseEntity<?> handleStopTransaction(
    //         @RequestBody String rawBody,
    //         @RequestHeader("IdDevice") String idDevice) {

    //     LocalDateTime receivedAt = LocalDateTime.now();
        
    //     try {
    //         // ✅ Validate header IdDevice
    //         idDeviceValidator.validate(idDevice);

    //         // ✅ Parse OCPP message
    //         var parsed = OcppMessageParser.parse(rawBody);
    //         if (parsed.messageTypeId() != 2 || !"StopTransaction".equalsIgnoreCase(parsed.action())) {
    //             return ResponseEntity.status(HttpStatus.BAD_REQUEST)
    //                     .body(Map.of("error", "Invalid message type or action. Expected StopTransaction"));
    //         }

    //         var payload = parsed.payload();
    //         Integer transactionId = payload.has("transactionId") ? payload.get("transactionId").asInt() : null;
    //         Long meterStop = payload.has("meterStop") ? payload.get("meterStop").asLong() : null;
    //         String timestamp = payload.has("timestamp") ? payload.get("timestamp").asText() : null;
    //         String idTag = payload.has("idTag") ? payload.get("idTag").asText() : null;

    //         if (transactionId == null) {
    //             return ResponseEntity.status(HttpStatus.BAD_REQUEST)
    //                     .body(Map.of("error", "Missing transactionId in payload"));
    //         }

    //         // ✅ Check session existence
    //         var sessionOpt = chargingSessionService.getSessionById(transactionId);
    //         if (sessionOpt == null || sessionOpt.getSessionId() == null) {
    //             Map<String, Object> idTagInfo = Map.of("status", "Invalid");
    //             Object[] ocppResponse = new Object[]{3, parsed.messageId(), Map.of("idTagInfo", idTagInfo)};
                
    //             OcppMessageLog log = new OcppMessageLog();
    //             log.setIdDevice(idDevice);
    //             log.setMessageId(parsed.messageId());
    //             log.setAction(parsed.action());
    //             log.setMessageTypeId(parsed.messageTypeId());
    //             log.setPayload(payload.toString());
    //             log.setResponse(new ObjectMapper().writeValueAsString(ocppResponse));
    //             log.setReceivedAt(receivedAt);
    //             log.setRespondedAt(LocalDateTime.now());
    //             messageLogRepo.save(log);
                
    //             return ResponseEntity.ok(ocppResponse);
    //         }

    //         // ✅ Validate IdTag belongs to same IdDevice if provided
    //         if (idTag != null && !idTag.isEmpty()) {
    //             var tagOpt = idTagInfoRepository.findByIdTagAndIdDevice(idTag, idDevice);

    //             if (tagOpt.isEmpty()) {
    //                 Map<String, Object> idTagInfo = Map.of("status", "Invalid");
    //                 Object[] ocppResponse = new Object[]{3, parsed.messageId(), Map.of("idTagInfo", idTagInfo)};
                    
    //                 OcppMessageLog log = new OcppMessageLog();
    //                 log.setIdDevice(idDevice);
    //                 log.setMessageId(parsed.messageId());
    //                 log.setAction(parsed.action());
    //                 log.setMessageTypeId(parsed.messageTypeId());
    //                 log.setPayload(payload.toString());
    //                 log.setResponse(new ObjectMapper().writeValueAsString(ocppResponse));
    //                 log.setReceivedAt(receivedAt);
    //                 log.setRespondedAt(LocalDateTime.now());
    //                 messageLogRepo.save(log);
                    
    //                 return ResponseEntity.ok(ocppResponse);
    //             }

    //             var tag = tagOpt.get();
    //             if (!"Accepted".equalsIgnoreCase(tag.getStatus())) {
    //                 Map<String, Object> idTagInfo = Map.of("status", tag.getStatus());
    //                 Object[] ocppResponse = new Object[]{3, parsed.messageId(), Map.of("idTagInfo", idTagInfo)};
                    
    //                 OcppMessageLog log = new OcppMessageLog();
    //                 log.setIdDevice(idDevice);
    //                 log.setMessageId(parsed.messageId());
    //                 log.setAction(parsed.action());
    //                 log.setMessageTypeId(parsed.messageTypeId());
    //                 log.setPayload(payload.toString());
    //                 log.setResponse(new ObjectMapper().writeValueAsString(ocppResponse));
    //                 log.setReceivedAt(receivedAt);
    //                 log.setRespondedAt(LocalDateTime.now());
    //                 messageLogRepo.save(log);
                    
    //                 return ResponseEntity.ok(ocppResponse);
    //             }
    //             if (tag.getExpiryDate().isBefore(LocalDateTime.now())) {
    //                 Map<String, Object> idTagInfo = Map.of("status", "Expired");
    //                 Object[] ocppResponse = new Object[]{3, parsed.messageId(), Map.of("idTagInfo", idTagInfo)};
                    
    //                 OcppMessageLog log = new OcppMessageLog();
    //                 log.setIdDevice(idDevice);
    //                 log.setMessageId(parsed.messageId());
    //                 log.setAction(parsed.action());
    //                 log.setMessageTypeId(parsed.messageTypeId());
    //                 log.setPayload(payload.toString());
    //                 log.setResponse(new ObjectMapper().writeValueAsString(ocppResponse));
    //                 log.setReceivedAt(receivedAt);
    //                 log.setRespondedAt(LocalDateTime.now());
    //                 messageLogRepo.save(log);
                    
    //                 return ResponseEntity.ok(ocppResponse);
    //             }
    //         }

    //         // ✅ Save meter values if present
    //         if (payload.has("transactionData")) {
    //             MeterValueRequest meterRequest = new MeterValueRequest();
    //             meterRequest.setConnectorId(1);
    //             meterRequest.setTransactionId(transactionId);

    //             var readings = new java.util.ArrayList<MeterValueRequest.MeterReading>();
    //             payload.get("transactionData").forEach(node -> {
    //                 MeterValueRequest.MeterReading reading = new MeterValueRequest.MeterReading();
    //                 reading.setTimestamp(node.get("timestamp").asText());
    //                 var samples = new java.util.ArrayList<MeterValueRequest.SampleReading>();
    //                 node.get("sampledValue").forEach(sv -> {
    //                     MeterValueRequest.SampleReading sr = new MeterValueRequest.SampleReading();
    //                     sr.setValue(sv.get("value").asText());
    //                     sr.setMeasurand(sv.has("measurand") ? sv.get("measurand").asText() : "Energy.Active.Import.Register");
    //                     samples.add(sr);
    //                 });
    //                 reading.setSampledValue(samples);
    //                 readings.add(reading);
    //             });
    //             meterRequest.setMeterValue(readings);
    //             meterValueService.saveMeterValues(meterRequest);
    //         }

    //         // ✅ Update session end info
    //         chargingSessionService.endChargingSession(transactionId, meterStop, timestamp);
            
    //         System.out.println("✅ Session " + transactionId + " ended. Total consumption: " + 
    //                         (meterStop != null ? meterStop + " kWh" : "N/A"));

    //         // Asynchronously call billing API to avoid delaying OCPP response
    //         final Integer finalTransactionId = transactionId;
    //         CompletableFuture.runAsync(() -> {
    //             try {
    //                 System.out.println("🔥🔥🔥 TRIGGERING BILLING for transaction: " + finalTransactionId);
    //                 Map<String, Object> billingResult = billingService.sendChargingDataToBilling(finalTransactionId);
    //                 System.out.println("📡 Billing API call result: " + billingResult);
                    
    //                 // Note: WebSocket messaging is handled inside BillingService
    //                 // You can add additional logging here if needed
                    
    //             } catch (Exception e) {
    //                 System.err.println("❌ Failed to call billing API for transaction " + finalTransactionId + ": " + e.getMessage());
    //                 e.printStackTrace();
    //             }
    //         });

    //         // ✅ Build OCPP response
    //         Map<String, Object> idTagInfo = Map.of("status", "Accepted");
    //         Object[] ocppResponse = new Object[]{
    //                 3,
    //                 parsed.messageId(),
    //                 Map.of("idTagInfo", idTagInfo)
    //         };

    //         // ✅ Create and save log
    //         OcppMessageLog log = new OcppMessageLog();
    //         log.setIdDevice(idDevice);
    //         log.setMessageId(parsed.messageId());
    //         log.setAction(parsed.action());
    //         log.setMessageTypeId(parsed.messageTypeId());
    //         log.setPayload(payload.toString());
    //         log.setResponse(new ObjectMapper().writeValueAsString(ocppResponse));
    //         log.setReceivedAt(receivedAt);
    //         log.setRespondedAt(LocalDateTime.now());
    //         messageLogRepo.save(log);

    //         return ResponseEntity.ok(ocppResponse);

    //     } catch (Exception e) {
    //         e.printStackTrace();
            
    //         try {
    //             var parsed = OcppMessageParser.parse(rawBody);
    //             OcppMessageLog log = new OcppMessageLog();
    //             log.setIdDevice(idDevice);
    //             log.setMessageId(parsed.messageId());
    //             log.setAction(parsed.action());
    //             log.setMessageTypeId(parsed.messageTypeId());
    //             log.setPayload(parsed.payload().toString());
    //             log.setResponse("{\"error\": \"" + e.getMessage() + "\"}");
    //             log.setReceivedAt(receivedAt);
    //             log.setRespondedAt(LocalDateTime.now());
    //             messageLogRepo.save(log);
    //             System.out.println("⚠️ StopTransaction error logged: " + e.getMessage());
    //         } catch (Exception logEx) {
    //             System.err.println("Failed to save error log: " + logEx.getMessage());
    //         }
            
    //         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
    //                 .body(Map.of("error", "INTERNAL_SERVER_ERROR", "message", e.getMessage()));
    //     }
    // }


    @PostMapping("/stopTransaction")
@Transactional
public ResponseEntity<?> handleStopTransaction(
        @RequestBody String rawBody,
        @RequestHeader("IdDevice") String idDevice) {

    LocalDateTime receivedAt = LocalDateTime.now();
    
    try {
        // ✅ Validate header IdDevice
        idDeviceValidator.validate(idDevice);

        // ✅ Parse OCPP message
        var parsed = OcppMessageParser.parse(rawBody);
        if (parsed.messageTypeId() != 2 || !"StopTransaction".equalsIgnoreCase(parsed.action())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid message type or action. Expected StopTransaction"));
        }

        var payload = parsed.payload();
        Integer transactionId = payload.has("transactionId") ? payload.get("transactionId").asInt() : null;
        Long meterStop = payload.has("meterStop") ? payload.get("meterStop").asLong() : null;
        String timestamp = payload.has("timestamp") ? payload.get("timestamp").asText() : null;
        String idTag = payload.has("idTag") ? payload.get("idTag").asText() : null;

        if (transactionId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Missing transactionId in payload"));
        }

        // ✅ Check session existence
        var sessionOpt = chargingSessionService.getSessionById(transactionId);
        if (sessionOpt == null || sessionOpt.getSessionId() == null) {
            Map<String, Object> idTagInfo = Map.of("status", "Invalid");
            Object[] ocppResponse = new Object[]{3, parsed.messageId(), Map.of("idTagInfo", idTagInfo)};
            
            OcppMessageLog log = new OcppMessageLog();
            log.setIdDevice(idDevice);
            log.setMessageId(parsed.messageId());
            log.setAction(parsed.action());
            log.setMessageTypeId(parsed.messageTypeId());
            log.setPayload(payload.toString());
            log.setResponse(new ObjectMapper().writeValueAsString(ocppResponse));
            log.setReceivedAt(receivedAt);
            log.setRespondedAt(LocalDateTime.now());
            messageLogRepo.save(log);
            
            return ResponseEntity.ok(ocppResponse);
        }

        // ✅ Validate IdTag belongs to same IdDevice if provided
        if (idTag != null && !idTag.isEmpty()) {
            // ✅ FIX: Use List instead of Optional
            List<IdTagInfo> tags = idTagInfoRepository.findByIdTagAndIdDevice(idTag, idDevice);
            
            if (tags.isEmpty()) {
                Map<String, Object> idTagInfo = Map.of("status", "Invalid");
                Object[] ocppResponse = new Object[]{3, parsed.messageId(), Map.of("idTagInfo", idTagInfo)};
                
                OcppMessageLog log = new OcppMessageLog();
                log.setIdDevice(idDevice);
                log.setMessageId(parsed.messageId());
                log.setAction(parsed.action());
                log.setMessageTypeId(parsed.messageTypeId());
                log.setPayload(payload.toString());
                log.setResponse(new ObjectMapper().writeValueAsString(ocppResponse));
                log.setReceivedAt(receivedAt);
                log.setRespondedAt(LocalDateTime.now());
                messageLogRepo.save(log);
                
                return ResponseEntity.ok(ocppResponse);
            }

            // Get the first tag (or most recent - you can sort by createdAt desc)
            IdTagInfo tag = tags.get(0);
            
            if (!"Accepted".equalsIgnoreCase(tag.getStatus())) {
                Map<String, Object> idTagInfo = Map.of("status", tag.getStatus());
                Object[] ocppResponse = new Object[]{3, parsed.messageId(), Map.of("idTagInfo", idTagInfo)};
                
                OcppMessageLog log = new OcppMessageLog();
                log.setIdDevice(idDevice);
                log.setMessageId(parsed.messageId());
                log.setAction(parsed.action());
                log.setMessageTypeId(parsed.messageTypeId());
                log.setPayload(payload.toString());
                log.setResponse(new ObjectMapper().writeValueAsString(ocppResponse));
                log.setReceivedAt(receivedAt);
                log.setRespondedAt(LocalDateTime.now());
                messageLogRepo.save(log);
                
                return ResponseEntity.ok(ocppResponse);
            }
            
            if (tag.getExpiryDate().isBefore(LocalDateTime.now())) {
                Map<String, Object> idTagInfo = Map.of("status", "Expired");
                Object[] ocppResponse = new Object[]{3, parsed.messageId(), Map.of("idTagInfo", idTagInfo)};
                
                OcppMessageLog log = new OcppMessageLog();
                log.setIdDevice(idDevice);
                log.setMessageId(parsed.messageId());
                log.setAction(parsed.action());
                log.setMessageTypeId(parsed.messageTypeId());
                log.setPayload(payload.toString());
                log.setResponse(new ObjectMapper().writeValueAsString(ocppResponse));
                log.setReceivedAt(receivedAt);
                log.setRespondedAt(LocalDateTime.now());
                messageLogRepo.save(log);
                
                return ResponseEntity.ok(ocppResponse);
            }
        }

        // ✅ Save meter values if present
        if (payload.has("transactionData")) {
            MeterValueRequest meterRequest = new MeterValueRequest();
            meterRequest.setConnectorId(1);
            meterRequest.setTransactionId(transactionId);

            var readings = new java.util.ArrayList<MeterValueRequest.MeterReading>();
            payload.get("transactionData").forEach(node -> {
                MeterValueRequest.MeterReading reading = new MeterValueRequest.MeterReading();
                reading.setTimestamp(node.get("timestamp").asText());
                var samples = new java.util.ArrayList<MeterValueRequest.SampleReading>();
                node.get("sampledValue").forEach(sv -> {
                    MeterValueRequest.SampleReading sr = new MeterValueRequest.SampleReading();
                    sr.setValue(sv.get("value").asText());
                    sr.setMeasurand(sv.has("measurand") ? sv.get("measurand").asText() : "Energy.Active.Import.Register");
                    samples.add(sr);
                });
                reading.setSampledValue(samples);
                readings.add(reading);
            });
            meterRequest.setMeterValue(readings);
            meterValueService.saveMeterValues(meterRequest);
        }

        // ✅ Update session end info
        chargingSessionService.endChargingSession(transactionId, meterStop, timestamp);
        
        System.out.println("✅ Session " + transactionId + " ended. Total consumption: " + 
                        (meterStop != null ? meterStop + " kWh" : "N/A"));

        // Asynchronously call billing API
        final Integer finalTransactionId = transactionId;
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("🔥🔥🔥 TRIGGERING BILLING for transaction: " + finalTransactionId);
                Map<String, Object> billingResult = billingService.sendChargingDataToBilling(finalTransactionId);
                System.out.println("📡 Billing API call result: " + billingResult);
            } catch (Exception e) {
                System.err.println("❌ Failed to call billing API for transaction " + finalTransactionId + ": " + e.getMessage());
                e.printStackTrace();
            }
        });

        // ✅ Build OCPP response
        Map<String, Object> idTagInfo = Map.of("status", "Accepted");
        Object[] ocppResponse = new Object[]{
                3,
                parsed.messageId(),
                Map.of("idTagInfo", idTagInfo)
        };

        // ✅ Create and save log
        OcppMessageLog log = new OcppMessageLog();
        log.setIdDevice(idDevice);
        log.setMessageId(parsed.messageId());
        log.setAction(parsed.action());
        log.setMessageTypeId(parsed.messageTypeId());
        log.setPayload(payload.toString());
        log.setResponse(new ObjectMapper().writeValueAsString(ocppResponse));
        log.setReceivedAt(receivedAt);
        log.setRespondedAt(LocalDateTime.now());
        messageLogRepo.save(log);

        return ResponseEntity.ok(ocppResponse);

    } catch (Exception e) {
        e.printStackTrace();
        
        try {
            var parsed = OcppMessageParser.parse(rawBody);
            OcppMessageLog log = new OcppMessageLog();
            log.setIdDevice(idDevice);
            log.setMessageId(parsed.messageId());
            log.setAction(parsed.action());
            log.setMessageTypeId(parsed.messageTypeId());
            log.setPayload(parsed.payload().toString());
            log.setResponse("{\"error\": \"" + e.getMessage() + "\"}");
            log.setReceivedAt(receivedAt);
            log.setRespondedAt(LocalDateTime.now());
            messageLogRepo.save(log);
            System.out.println("⚠️ StopTransaction error logged: " + e.getMessage());
        } catch (Exception logEx) {
            System.err.println("Failed to save error log: " + logEx.getMessage());
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "INTERNAL_SERVER_ERROR", "message", e.getMessage()));
    }
}
    
    @PostMapping("/bootNotification")
    public ResponseEntity<?> handleBootNotification(
            @RequestBody String rawBody,
            @RequestHeader("IdDevice") String headerIdDevice) {

        LocalDateTime receivedAt = LocalDateTime.now();
        try {
            idDeviceValidator.validate(headerIdDevice);
            var parsed = OcppMessageParser.parse(rawBody);
            if (parsed.messageTypeId() != 2 || !"BootNotification".equalsIgnoreCase(parsed.action())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid message type or action. Expected BootNotification"));
            }

            var payload = parsed.payload();
            String meterSerialNumber = payload.has("meterSerialNumber") ? payload.get("meterSerialNumber").asText() : null;
            if (meterSerialNumber == null || meterSerialNumber.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Missing meterSerialNumber"));
            }
            if (!headerIdDevice.equals(meterSerialNumber)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Header and payload mismatch"));
            }

            ObjectNode responsePayload = ocppActionService.handleBootNotification(headerIdDevice, payload);
            Object[] ocppResponse = new Object[]{3, parsed.messageId(), responsePayload};

            OcppMessageLog log = new OcppMessageLog();
            log.setIdDevice(headerIdDevice);
            log.setMessageId(parsed.messageId());
            log.setAction(parsed.action());
            log.setMessageTypeId(parsed.messageTypeId());
            log.setPayload(payload.toString());
            log.setResponse(new ObjectMapper().writeValueAsString(ocppResponse));
            log.setReceivedAt(receivedAt);
            log.setRespondedAt(LocalDateTime.now());
            messageLogRepo.save(log);

            return ResponseEntity.ok(ocppResponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_SERVER_ERROR", "message", e.getMessage()));
        }
    }
}
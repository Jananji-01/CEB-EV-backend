// package com.example.EVProject.services;

// import com.example.EVProject.model.*;
// import com.example.EVProject.repositories.*;
// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.fasterxml.jackson.databind.node.ArrayNode;
// import com.fasterxml.jackson.databind.node.ObjectNode;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.messaging.simp.SimpMessagingTemplate;
// import org.springframework.stereotype.Service;

// import java.time.LocalDateTime;
// import java.time.ZoneOffset;
// import java.time.ZonedDateTime;
// import java.time.format.DateTimeFormatter;
// import java.util.*;

// @Service
// public class OcppMessageProcessor {

//     @Autowired
//     private ChargingSessionRepository chargingSessionRepository;
//     @Autowired
//     private IdTagInfoRepository idTagInfoRepository;
//     @Autowired
//     private SmartPlugRepository smartPlugRepository;
//     @Autowired
//     private EvOwnerRepository evOwnerRepository;
//     @Autowired
//     private MeterValueRepository meterValueRepository;
//     @Autowired
//     private SampledValueRepository sampledValueRepository;
//     @Autowired
//     private OcppMessageLogRepository messageLogRepository;
//     @Autowired
//     private SimpMessagingTemplate messagingTemplate;

//     private final ObjectMapper objectMapper = new ObjectMapper();
//     private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

//     /**
//      * Process incoming OCPP WebSocket message
//      */
//     public String processMessage(String deviceId, String message) {
//         try {
//             JsonNode root = objectMapper.readTree(message);

//             if (!root.isArray() || root.size() < 3) {
//                 return createError("ProtocolError", "Invalid message format", null);
//             }

//             int messageTypeId = root.get(0).asInt();
//             String messageId = root.get(1).asText();
//             String action = root.get(2).asText();
//             JsonNode payload = root.size() > 3 ? root.get(3) : objectMapper.createObjectNode();

//             // Log incoming message
//             logMessage(deviceId, messageId, action, messageTypeId, payload.toString(), "INCOMING");

//             // Process based on message type
//             switch (messageTypeId) {
//                 case 2: // CALL
//                     return handleCall(deviceId, messageId, action, payload);
//                 case 3: // CALLRESULT
//                     return handleCallResult(deviceId, messageId, payload);
//                 case 4: // CALLERROR
//                     return handleCallError(deviceId, messageId, payload);
//                 default:
//                     return createError("FormatViolation", "Unknown message type", messageId);
//             }

//         } catch (Exception e) {
//             e.printStackTrace();
//             return createError("InternalError", e.getMessage(), null);
//         }
//     }

//     /**
//      * Handle OCPP CALL messages (device → server)
//      */
//     private String handleCall(String deviceId, String messageId, String action, JsonNode payload) {
//         ObjectNode responsePayload = objectMapper.createObjectNode();

//         switch (action) {
//             case "BootNotification":
//                 responsePayload = handleBootNotification(deviceId, payload);
//                 break;
//             case "Authorize":
//                 handleAuthorize(deviceId, payload, messageId);
//                 break;
//             case "StartTransaction":
//                 responsePayload = handleStartTransaction(deviceId, payload);
//                 break;
//             case "StopTransaction":
//                 responsePayload = handleStopTransaction(deviceId, payload);
//                 break;
//             case "MeterValues":
//                 responsePayload = handleMeterValues(deviceId, payload);
//                 break;
//             case "Heartbeat":
//                 responsePayload = handleHeartbeat();
//                 break;
//             case "StatusNotification":
//                 responsePayload = handleStatusNotification(deviceId, payload);
//                 break;
//             default:
//                 responsePayload = createErrorPayload("NotSupported", "Action not supported");
//         }

//         // Create CALLRESULT response
//         String response = createCallResult(messageId, responsePayload);

//         // Log response
//         logMessage(deviceId, messageId, action, 3, responsePayload.toString(), "OUTGOING");

//         return response;
//     }

//     /**
//      * 1. BootNotification Handler - Matches REST API format
//      */
//     private ObjectNode handleBootNotification(String deviceId, JsonNode payload) {
//         ObjectNode response = objectMapper.createObjectNode();

//         try {
//             // Extract payload fields
//             String model = payload.has("chargePointModel") ? payload.get("chargePointModel").asText() : null;
//             String vendor = payload.has("chargePointVendor") ? payload.get("chargePointVendor").asText() : null;
//             String firmware = payload.has("firmwareVersion") ? payload.get("firmwareVersion").asText() : null;

//             // Update device information
//             SmartPlug plug = smartPlugRepository.findById(deviceId)
//                     .orElseGet(() -> {
//                         SmartPlug newPlug = new SmartPlug();
//                         newPlug.setIdDevice(deviceId);
//                         return newPlug;
//                     });

//             plug.setChargePointModel(payload.path("chargePointModel").asText("Unknown"));
//             plug.setChargePointVendor(payload.path("chargePointVendor").asText("Unknown"));
//             plug.setFirmwareVersion(payload.path("firmwareVersion").asText("1.0.0"));

//             smartPlugRepository.save(plug);

//             // Build OCPP response
//             response.put("status", "Accepted");
//             response.put("currentTime", ZonedDateTime.now(ZoneOffset.UTC).toString());
//             response.put("interval", 300); // Heartbeat interval in seconds

//         } catch (Exception e) {
//             response.put("status", "Rejected");
//         }

//         return response;
//     }

//     /**
//      * 2. Authorize Handler – uses IdDevice field
//      * Returns the persistent IdTag from id_tag_info if a valid (non‑expired) record exists.
//      * Does NOT create a new record – that is done only during QR scan.
//      */
//     // private ObjectNode handleAuthorize(String deviceId, JsonNode payload) {
//     //     ObjectNode response = objectMapper.createObjectNode();
//     //     ObjectNode idTagInfo = objectMapper.createObjectNode();

//     //     try {
//     //         String deviceIdFromPayload = payload.path("IdDevice").asText();
            
//     //         // Validate device ID
//     //         if (deviceIdFromPayload == null || deviceIdFromPayload.isEmpty()) {
//     //             idTagInfo.put("status", "Invalid");
//     //             System.err.println("❌ Authorize: missing IdDevice in payload");
//     //         } else if (!deviceId.equals(deviceIdFromPayload)) {
//     //             idTagInfo.put("status", "Invalid");
//     //             System.err.println("❌ Authorize: deviceId mismatch (header=" + deviceId + ", payload=" + deviceIdFromPayload + ")");
//     //         } else {
//     //             LocalDateTime now = LocalDateTime.now();
//     //             var tagOpt = idTagInfoRepository.findTopByIdDeviceOrderByCreatedAtDesc(deviceIdFromPayload);

//     //             if (tagOpt.isPresent()) {
//     //                 IdTagInfo tag = tagOpt.get();
//     //                 if (tag.getExpiryDate().isAfter(now)) {
//     //                     // Valid record found
//     //                     idTagInfo.put("status", "Accepted");
//     //                     idTagInfo.put("expiryDate", tag.getExpiryDate().atZone(ZoneOffset.UTC).toString());
//     //                     idTagInfo.put("IdTag", tag.getIdTag());
//     //                     System.out.println("✅ Authorize: returning idTag " + tag.getIdTag() + " for device " + deviceId);
//     //                 } else {
//     //                     // Record expired
//     //                     idTagInfo.put("status", "Invalid");
//     //                     System.out.println("⚠️ Authorize: latest IdTagInfo expired for device " + deviceId);
//     //                 }
//     //             } else {
//     //                 // No record at all
//     //                 idTagInfo.put("status", "Invalid");
//     //                 System.out.println("❌ Authorize: no IdTagInfo found for device " + deviceId);
//     //             }
//     //         }
//     //     } catch (Exception e) {
//     //         idTagInfo.put("status", "Invalid");
//     //         System.err.println("❌ Authorize error for device " + deviceId + ": " + e.getMessage());
//     //         e.printStackTrace();
//     //     }

//     //     response.set("idTagInfo", idTagInfo);
//     //     return response;
//     // }

//     /**
//      * 2. Authorize Handler – matches REST API response format
//      * Returns the persistent IdTag from id_tag_info if a valid (non‑expired) record exists.
//      * Creates a new IdTag if no valid one exists.
//      */
//     private Object[] handleAuthorize(String deviceId, JsonNode payload, String messageId) {
//         ObjectNode idTagInfo = objectMapper.createObjectNode();

//         try {
//             String deviceIdFromPayload = payload.path("IdDevice").asText();
            
//             // 1️⃣ Validate payload IdDevice
//             if (deviceIdFromPayload == null || deviceIdFromPayload.isEmpty()) {
//                 idTagInfo.put("status", "Invalid");
//                 System.err.println("❌ Authorize: missing IdDevice in payload");
//             } else if (!deviceId.equals(deviceIdFromPayload)) {
//                 idTagInfo.put("status", "Invalid");
//                 System.err.println("❌ Authorize: deviceId mismatch (header=" + deviceId + ", payload=" + deviceIdFromPayload + ")");
//             } else {
//                 LocalDateTime now = LocalDateTime.now();
                
//                 // 2️⃣ Find valid (non-expired) tag
//                 List<IdTagInfo> existingTags = idTagInfoRepository.findByIdDevice(deviceIdFromPayload);
//                 IdTagInfo validTag = null;
                
//                 for (IdTagInfo tag : existingTags) {
//                     if (tag.getExpiryDate() != null && tag.getExpiryDate().isAfter(now)) {
//                         validTag = tag;
//                         break;
//                     }
//                 }
                
//                 if (validTag != null) {
//                     // ✅ Reuse existing valid tag - same as REST API
//                     idTagInfo.put("status", "Accepted");
//                     idTagInfo.put("expiryDate", validTag.getExpiryDate().toString() + "Z");
//                     idTagInfo.put("IdTag", validTag.getIdTag());
//                     System.out.println("✅ Authorize: reusing idTag " + validTag.getIdTag() + " for device " + deviceId);
                    
//                 } else {
//                     // ❌ No valid tag found - create new one (same as REST API)
//                     SmartPlug plug = smartPlugRepository.findById(deviceIdFromPayload)
//                             .orElseThrow(() -> new IllegalArgumentException("IdDevice not found: " + deviceIdFromPayload));
                    
//                     String accountReference = (plug.getCebSerialNo() != null)
//                             ? plug.getCebSerialNo()
//                             : plug.getIdDevice();
                    
//                     String idTag = generateIdTag(accountReference);
//                     LocalDateTime expiryDate = now.plusHours(6); // 6 hours expiry
                    
//                     // Create and save new IdTagInfo record
//                     IdTagInfo newTag = new IdTagInfo();
//                     newTag.setIdDevice(deviceIdFromPayload);
//                     newTag.setIdTag(idTag);
//                     newTag.setStatus("Accepted");
//                     newTag.setExpiryDate(expiryDate);
//                     newTag.setCreatedAt(now);
//                     idTagInfoRepository.save(newTag);
                    
//                     // Build response
//                     idTagInfo.put("status", "Accepted");
//                     idTagInfo.put("expiryDate", expiryDate.toString() + "Z");
//                     idTagInfo.put("IdTag", idTag);
//                     System.out.println("✅ Authorize: created new idTag " + idTag + " for device " + deviceId);
//                 }
//             }
//         } catch (Exception e) {
//             idTagInfo.put("status", "Invalid");
//             System.err.println("❌ Authorize error for device " + deviceId + ": " + e.getMessage());
//             e.printStackTrace();
//         }

//         // Build OCPP response in the exact format your REST API uses
//         ObjectNode payloadNode = objectMapper.createObjectNode();
//         payloadNode.set("idTagInfo", idTagInfo);
        
//         return new Object[]{
//                 3,                    // MessageTypeId for CALLRESULT
//                 messageId,            // Message ID
//                 payloadNode           // Payload with idTagInfo
//         };
//     }

//     /**
//      * 3. StartTransaction Handler
//      */
//     private ObjectNode handleStartTransaction(String deviceId, JsonNode payload) {
//         ObjectNode response = objectMapper.createObjectNode();
//         ObjectNode idTagInfo = objectMapper.createObjectNode();

//         try {
//             String idTag = payload.path("idTag").asText();
//             int connectorId = payload.path("connectorId").asInt(1);
//             long meterStart = payload.path("meterStart").asLong(0);

//             // Validate idTag
//             var tagOpt = idTagInfoRepository.findByIdTagAndIdDevice(idTag, deviceId);
//             if (tagOpt.isEmpty() || !"Accepted".equals(tagOpt.get().getStatus())) {
//                 idTagInfo.put("status", "Invalid");
//                 response.set("idTagInfo", idTagInfo);
//                 return response;
//             }

//             // Check for existing active session
//             if (chargingSessionRepository.findByIdDeviceAndEndTimeIsNull(deviceId).isPresent()) {
//                 idTagInfo.put("status", "ConcurrentTx");
//                 response.set("idTagInfo", idTagInfo);
//                 return response;
//             }

//             // Retrieve EV owner account number using the idTag
//             String evOwnerAccountNo = null;
//             Optional<EvOwner> ownerOpt = evOwnerRepository.findByIdTag(idTag);
//             if (ownerOpt.isPresent()) {
//                 evOwnerAccountNo = ownerOpt.get().getEAccountNumber();
//                 System.out.println("💾 [OCPP] Found owner account " + evOwnerAccountNo + " for idTag " + idTag);
//             } else {
//                 System.out.println("⚠️ [OCPP] No EV owner found for idTag " + idTag);
//             }

//             // Create new charging session
//             ChargingSession session = new ChargingSession();
//             session.setIdDevice(deviceId);
//             session.setStartTime(LocalDateTime.now());
//             session.setChargingMode("FAST");
//             session.setTotalConsumption(0.0);
//             session.setAmount(0.0);
//             session.setSoc(0.0);
//             session.setEvOwnerAccountNo(evOwnerAccountNo);

//             ChargingSession savedSession = chargingSessionRepository.save(session);

//             idTagInfo.put("status", "Accepted");
//             response.set("idTagInfo", idTagInfo);
//             response.put("transactionId", savedSession.getSessionId());

//             // Send WebSocket message to frontend
//             Map<String, Object> frontendMessage = new HashMap<>();
//             frontendMessage.put("type", "TRANSACTION_STARTED");
//             frontendMessage.put("transactionId", savedSession.getSessionId());
//             frontendMessage.put("idDevice", deviceId);
//             frontendMessage.put("timestamp", LocalDateTime.now().toString());

//             messagingTemplate.convertAndSend("/topic/device/" + deviceId, frontendMessage);
//             System.out.println("✅ Sent TRANSACTION_STARTED to frontend: " + frontendMessage);

//         } catch (Exception e) {
//             idTagInfo.put("status", "InternalError");
//             response.set("idTagInfo", idTagInfo);
//             e.printStackTrace();
//         }

//         return response;
//     }

//     /**
//      * 4. MeterValues Handler
//      */
//     private ObjectNode handleMeterValues(String deviceId, JsonNode payload) {
//         ObjectNode response = objectMapper.createObjectNode();

//         try {
//             int connectorId = payload.path("connectorId").asInt(1);
//             Integer transactionId = payload.path("transactionId").asInt();
//             JsonNode meterValueArray = payload.path("meterValue");

//             System.out.println("📊 [DEBUG] Processing MeterValues for device: " + deviceId);
//             System.out.println("📊 [DEBUG] Transaction ID: " + transactionId);

//             if (transactionId == null) {
//                 System.out.println("❌ [DEBUG] No transaction ID in MeterValues");
//                 response.put("status", "Rejected");
//                 return response;
//             }

//             // Get the charging session
//             var sessionOpt = chargingSessionRepository.findById(transactionId);
//             if (sessionOpt.isEmpty()) {
//                 System.out.println("❌ [DEBUG] Session not found for transaction: " + transactionId);
//                 response.put("status", "Rejected");
//                 return response;
//             }

//             ChargingSession session = sessionOpt.get();

//             // Process each meter value
//             if (meterValueArray.isArray()) {
//                 for (JsonNode meterValue : meterValueArray) {
//                     String timestampStr = meterValue.path("timestamp").asText();
//                     JsonNode sampledValues = meterValue.path("sampledValue");

//                     System.out.println("📊 [DEBUG] Processing timestamp: " + timestampStr);

//                     if (sampledValues.isArray()) {
//                         for (JsonNode sample : sampledValues) {
//                             String measurand = sample.path("measurand").asText("Energy.Active.Import.Register");
//                             String value = sample.path("value").asText();
//                             String unit = sample.path("unit").asText("kWh");

//                             System.out.println("📊 [DEBUG] Sample - Measurand: " + measurand + ", Value: " + value + ", Unit: " + unit);

//                             // Extract numeric value
//                             double numericValue = 0.0;
//                             try {
//                                 numericValue = Double.parseDouble(value);
//                             } catch (NumberFormatException e) {
//                                 System.err.println("❌ [DEBUG] Invalid value format: " + value);
//                                 continue;
//                             }

//                             // Send real-time data to frontend via WebSocket
//                             Map<String, Object> realtimeData = new HashMap<>();
//                             realtimeData.put("type", "METER_VALUES");
//                             realtimeData.put("idDevice", deviceId);
//                             realtimeData.put("transactionId", transactionId);
//                             realtimeData.put("timestamp", timestampStr);

//                             // Add specific measurements
//                             if ("Energy.Active.Import.Register".equals(measurand)) {
//                                 realtimeData.put("energy", numericValue);
//                                 realtimeData.put("totalConsumption", numericValue);

//                                 // Update session total consumption
//                                 session.setTotalConsumption(numericValue);
//                                 chargingSessionRepository.save(session);

//                             } else if ("Power.Active.Import".equals(measurand) || "Power.Active.Import.Register".equals(measurand)) {
//                                 realtimeData.put("power", numericValue);
//                             } else if ("Voltage".equals(measurand)) {
//                                 realtimeData.put("voltage", numericValue);
//                             } else if ("Current.Import".equals(measurand)) {
//                                 realtimeData.put("current", numericValue);
//                             } else if ("Temperature".equals(measurand)) {
//                                 realtimeData.put("temperature", numericValue);
//                             }

//                             // Send to frontend
//                             sendMeterValueToFrontend(realtimeData);
//                         }
//                     }
//                 }
//             }

//             return objectMapper.createObjectNode();

//         } catch (Exception e) {
//             ObjectNode errorResponse = objectMapper.createObjectNode();
//             errorResponse.put("status", "Rejected");
//             return errorResponse;
//         }

//     }

//     /**
//      * Send meter values to frontend via STOMP
//      */
//     private void sendMeterValueToFrontend(Map<String, Object> meterData) {
//         try {
//             String deviceId = (String) meterData.get("idDevice");

//             if (deviceId == null) {
//                 System.err.println("❌ [DEBUG] Cannot send meter data: deviceId is null");
//                 return;
//             }

//             // Create a properly formatted message for frontend
//             Map<String, Object> frontendMessage = new HashMap<>();
//             frontendMessage.put("type", "METER_VALUES");
//             frontendMessage.put("idDevice", deviceId);
//             frontendMessage.put("timestamp", meterData.get("timestamp"));

//             // Add sampled values in the format frontend expects
//             List<Map<String, Object>> sampledValues = new ArrayList<>();

//             // Add power data if available
//             if (meterData.containsKey("power")) {
//                 Map<String, Object> powerSample = new HashMap<>();
//                 powerSample.put("value", meterData.get("power").toString());
//                 powerSample.put("measurand", "Power.Active.Import");
//                 powerSample.put("unit", "kW");
//                 sampledValues.add(powerSample);
//             }

//             // Add voltage data if available
//             if (meterData.containsKey("voltage")) {
//                 Map<String, Object> voltageSample = new HashMap<>();
//                 voltageSample.put("value", meterData.get("voltage").toString());
//                 voltageSample.put("measurand", "Voltage");
//                 voltageSample.put("unit", "V");
//                 sampledValues.add(voltageSample);
//             }

//             // Add current data if available
//             if (meterData.containsKey("current")) {
//                 Map<String, Object> currentSample = new HashMap<>();
//                 currentSample.put("value", meterData.get("current").toString());
//                 currentSample.put("measurand", "Current.Import");
//                 currentSample.put("unit", "A");
//                 sampledValues.add(currentSample);
//             }

//             // Add energy data if available
//             if (meterData.containsKey("energy")) {
//                 Map<String, Object> energySample = new HashMap<>();
//                 energySample.put("value", meterData.get("energy").toString());
//                 energySample.put("measurand", "Energy.Active.Import.Register");
//                 energySample.put("unit", "kWh");
//                 sampledValues.add(energySample);
//             }

//             // Create meterValue object
//             Map<String, Object> meterValueObj = new HashMap<>();
//             meterValueObj.put("sampledValue", sampledValues);
//             frontendMessage.put("meterValue", meterValueObj);

//             System.out.println("📤 [DEBUG] Sending to frontend device " + deviceId + ": " + frontendMessage);

//             // Send to device-specific topic
//             messagingTemplate.convertAndSend("/topic/device/" + deviceId, frontendMessage);

//             // Also send to general charging topic
//             messagingTemplate.convertAndSend("/topic/charging", frontendMessage);

//         } catch (Exception e) {
//             System.err.println("❌ [DEBUG] Error sending meter data to frontend: " + e.getMessage());
//             e.printStackTrace();
//         }
//     }

//     /**
//      * 5. StopTransaction Handler
//      */
//     private ObjectNode handleStopTransaction(String deviceId, JsonNode payload) {
//         ObjectNode response = objectMapper.createObjectNode();
//         ObjectNode idTagInfo = objectMapper.createObjectNode();

//         try {
//             Integer transactionId = payload.path("transactionId").asInt();
//             Long meterStop = payload.path("meterStop").asLong();
//             String timestampStr = payload.path("timestamp").asText();
//             String idTag = payload.has("idTag") ? payload.path("idTag").asText() : null;

//             // Get session
//             var sessionOpt = chargingSessionRepository.findById(transactionId);
//             if (sessionOpt.isEmpty()) {
//                 idTagInfo.put("status", "Invalid");
//                 response.set("idTagInfo", idTagInfo);
//                 return response;
//             }

//             ChargingSession session = sessionOpt.get();

//             // Verify device ownership
//             if (!deviceId.equals(session.getIdDevice())) {
//                 idTagInfo.put("status", "Invalid");
//                 response.set("idTagInfo", idTagInfo);
//                 return response;
//             }

//             // Validate IdTag if provided - with expiry check
//             if (idTag != null && !idTag.isEmpty()) {
//                 var tagOpt = idTagInfoRepository.findByIdTagAndIdDevice(idTag, deviceId);
//                 LocalDateTime now = LocalDateTime.now();
                
//                 if (tagOpt.isEmpty()) {
//                     idTagInfo.put("status", "Invalid");
//                     System.out.println("❌ StopTransaction: IdTag not found: " + idTag);
//                     response.set("idTagInfo", idTagInfo);
//                     return response;
//                 }

//                 var tag = tagOpt.get();
                
//                 // Check expiry - same logic as REST API
//                 if (tag.getExpiryDate().isBefore(now)) {
//                     idTagInfo.put("status", "Expired");
//                     System.out.println("❌ StopTransaction: IdTag expired: " + idTag + 
//                                      " (expired: " + tag.getExpiryDate() + ")");
//                     response.set("idTagInfo", idTagInfo);
//                     return response;
//                 }
                
//                 // Check status
//                 if (!"Accepted".equals(tag.getStatus())) {
//                     idTagInfo.put("status", tag.getStatus());
//                     System.out.println("❌ StopTransaction: IdTag status invalid: " + tag.getStatus());
//                     response.set("idTagInfo", idTagInfo);
//                     return response;
//                 }
//             }

//             // Process transactionData if present (for meter values)
//             if (payload.has("transactionData")) {
//                 JsonNode transactionData = payload.path("transactionData");
//                 // Process meter values similar to MeterValues handler
//                 // This would be implemented similarly to MeterValues
//             }

//             // Calculate duration
//             // USE SERVER TIME instead of device timestamp
//             LocalDateTime endTime = LocalDateTime.now();
//             System.out.println("⏰ [DEBUG] Using server end time: " + endTime);
//             System.out.println("⏰ [DEBUG] Session start time: " + session.getStartTime());

//             // Calculate duration in minutes
//             long durationMinutes = 0;
//             if (session.getStartTime() != null) {
//                 durationMinutes = java.time.Duration.between(session.getStartTime(), endTime).toMinutes();
//             }

//             // Calculate cost at $0.15 per kWh
//             double cost = 0.0;
//             if (meterStop != null) {
//                 double consumptionKWh = meterStop.doubleValue();
//                 cost = consumptionKWh * 0.15; // $0.15 per kWh

//                 // Update session with final values
//                 session.setTotalConsumption(consumptionKWh);
//                 session.setAmount(cost);
//                 session.setEndTime(endTime);

//                 chargingSessionRepository.save(session);

//                 System.out.println("💰 Transaction completed:");
//                 System.out.println("   - Consumption: " + consumptionKWh + " kWh");
//                 System.out.println("   - Duration: " + durationMinutes + " minutes");
//                 System.out.println("   - Cost: $" + String.format("%.2f", cost));

//                 // Send transaction details to frontend
//                 Map<String, Object> transactionDetails = new HashMap<>();
//                 transactionDetails.put("type", "TRANSACTION_COMPLETED");
//                 transactionDetails.put("transactionId", transactionId);
//                 transactionDetails.put("idDevice", deviceId);
//                 transactionDetails.put("powerConsumed", consumptionKWh);
//                 transactionDetails.put("durationMinutes", durationMinutes);
//                 transactionDetails.put("cost", String.format("%.2f", cost));
//                 transactionDetails.put("startTime", session.getStartTime().toString());
//                 transactionDetails.put("endTime", endTime.toString());
//                 transactionDetails.put("timestamp", LocalDateTime.now().toString());

//                 messagingTemplate.convertAndSend("/topic/device/" + deviceId, transactionDetails);
//                 messagingTemplate.convertAndSend("/topic/charging", transactionDetails);
//             }

//             idTagInfo.put("status", "Accepted");
//             response.set("idTagInfo", idTagInfo);

//         } catch (Exception e) {
//             idTagInfo.put("status", "InternalError");
//             response.set("idTagInfo", idTagInfo);
//             System.err.println("❌ Error in handleStopTransaction: " + e.getMessage());
//         }

//         return response;

//     }

//     /**
//      * Heartbeat Handler – return current time in UTC with Z
//      */
//     private ObjectNode handleHeartbeat() {
//         ObjectNode response = objectMapper.createObjectNode();
//         response.put("currentTime", ZonedDateTime.now(ZoneOffset.UTC).toString());
//         return response;
//     }

//     /**
//      * StatusNotification Handler – persist device status
//      */
//     private ObjectNode handleStatusNotification(String deviceId, JsonNode payload) {
//         ObjectNode response = objectMapper.createObjectNode();
//         try {
//             String status = payload.path("status").asText();
//             // Update smart_plug status
//             smartPlugRepository.findById(deviceId).ifPresent(plug -> {
//                 plug.setStatus(status);
//                 smartPlugRepository.save(plug);
//             });
//             response.put("status", "Accepted");
//         } catch (Exception e) {
//             response.put("status", "Rejected");
//         }
//         return response;
//     }

//     private String handleCallResult(String deviceId, String messageId, JsonNode payload) {
//         // Handle responses to our calls (like RemoteStartTransaction.conf)
//         // You can implement callback logic here
//         return null;
//     }

//     private String handleCallError(String deviceId, String messageId, JsonNode payload) {
//         // Handle error responses
//         return null;
//     }

//     private String createCallResult(String messageId, JsonNode payload) {
//         ArrayNode response = objectMapper.createArrayNode();
//         response.add(3); // MessageTypeId for CALLRESULT
//         response.add(messageId);
//         response.add(payload);
//         return response.toString();
//     }

//     private String createError(String errorCode, String errorDescription, String messageId) {
//         ArrayNode response = objectMapper.createArrayNode();
//         response.add(4); // MessageTypeId for CALLERROR
//         response.add(messageId != null ? messageId : "");
//         response.add(errorCode);
//         response.add(errorDescription);
//         response.add(objectMapper.createObjectNode()); // Empty error details
//         return response.toString();
//     }

//     private ObjectNode createErrorPayload(String status, String message) {
//         ObjectNode payload = objectMapper.createObjectNode();
//         payload.put("status", status);
//         if (message != null) {
//             payload.put("message", message);
//         }
//         return payload;
//     }

//     private void logMessage(String deviceId, String messageId, String action,
//                             int messageTypeId, String payload, String direction) {
//         try {
//             OcppMessageLog log = new OcppMessageLog();
//             log.setIdDevice(deviceId);
//             log.setMessageId(messageId);
//             log.setAction(action);
//             log.setMessageTypeId(messageTypeId);
//             log.setPayload(payload);
//             log.setReceivedAt(LocalDateTime.now());
//             messageLogRepository.save(log);
//         } catch (Exception e) {
//             // Silent fail on logging errors
//         }
//     }

//     /**
//      * Generate a unique IdTag for a device - matches REST API logic
//      */
//     private String generateIdTag(String baseValue) {
//         try {
//             java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
//             byte[] hash = digest.digest((baseValue + System.currentTimeMillis()).getBytes());
//             String hex = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
//             return "IDT-" + hex.substring(0, 8).toUpperCase();
//         } catch (Exception e) {
//             return "IDT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
//         }
//     }

//     /**
//      * Helper method to check if IdTag is valid and not expired
//      */
//     private boolean isValidIdTag(String idTag, String deviceId) {
//         Optional<IdTagInfo> tagOpt = idTagInfoRepository.findByIdTagAndIdDevice(idTag, deviceId);
//         if (tagOpt.isEmpty()) {
//             return false;
//         }
        
//         IdTagInfo tag = tagOpt.get();
//         LocalDateTime now = LocalDateTime.now();
        
//         // Check expiry and status
//         return tag.getExpiryDate().isAfter(now) && "Accepted".equals(tag.getStatus());
//     }

//     /**
//      * Get or create valid IdTag for device - matches REST API logic
//      */
//     private IdTagInfo getOrCreateValidIdTag(String deviceId) {
//         LocalDateTime now = LocalDateTime.now();
        
//         // Check for existing valid tag
//         List<IdTagInfo> existingTags = idTagInfoRepository.findByIdDevice(deviceId);
//         for (IdTagInfo tag : existingTags) {
//             if (tag.getExpiryDate().isAfter(now) && "Accepted".equals(tag.getStatus())) {
//                 return tag;
//             }
//         }
        
//         // Create new tag with 6 hours expiry
//         SmartPlug plug = smartPlugRepository.findById(deviceId)
//                 .orElseThrow(() -> new IllegalArgumentException("IdDevice not found: " + deviceId));
        
//         String accountReference = (plug.getCebSerialNo() != null)
//                 ? plug.getCebSerialNo()
//                 : plug.getIdDevice();
        
//         String idTag = generateIdTag(accountReference);
//         LocalDateTime expiryDate = now.plusHours(6);
        
//         IdTagInfo newTag = new IdTagInfo();
//         newTag.setIdDevice(deviceId);
//         newTag.setIdTag(idTag);
//         newTag.setStatus("Accepted");
//         newTag.setExpiryDate(expiryDate);
//         newTag.setCreatedAt(now);
        
//         return idTagInfoRepository.save(newTag);
//     }
// }

package com.example.EVProject.services;

import com.example.EVProject.model.*;
import com.example.EVProject.repositories.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OcppMessageProcessor {

    @Autowired
    private ChargingSessionRepository chargingSessionRepository;
    @Autowired
    private IdTagInfoRepository idTagInfoRepository;
    @Autowired
    private SmartPlugRepository smartPlugRepository;
    @Autowired
    private EvOwnerRepository evOwnerRepository;
    @Autowired
    private MeterValueRepository meterValueRepository;
    @Autowired
    private SampledValueRepository sampledValueRepository;
    @Autowired
    private OcppMessageLogRepository messageLogRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // OcppWebSocketService is no longer used – removed

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * Process incoming OCPP WebSocket message
     */
    public String processMessage(String deviceId, String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            if (!root.isArray() || root.size() < 3) {
                return createError("ProtocolError", "Invalid message format", null);
            }

            int messageTypeId = root.get(0).asInt();
            String messageId = root.get(1).asText();
            String action = root.get(2).asText();
            JsonNode payload = root.size() > 3 ? root.get(3) : objectMapper.createObjectNode();

            // Log incoming message
            logMessage(deviceId, messageId, action, messageTypeId, payload.toString(), "INCOMING");

            // Process based on message type
            switch (messageTypeId) {
                case 2: // CALL
                    return handleCall(deviceId, messageId, action, payload);
                case 3: // CALLRESULT
                    return handleCallResult(deviceId, messageId, payload);
                case 4: // CALLERROR
                    return handleCallError(deviceId, messageId, payload);
                default:
                    return createError("FormatViolation", "Unknown message type", messageId);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return createError("InternalError", e.getMessage(), null);
        }
    }

    /**
     * Handle OCPP CALL messages (device → server)
     */
    private String handleCall(String deviceId, String messageId, String action, JsonNode payload) {
        ObjectNode responsePayload;

        switch (action) {
            case "BootNotification":
                responsePayload = handleBootNotification(deviceId, payload);
                break;
            case "Authorize":
                responsePayload = handleAuthorize(deviceId, payload);
                break;
            case "StartTransaction":
                responsePayload = handleStartTransaction(deviceId, payload);
                break;
            case "StopTransaction":
                responsePayload = handleStopTransaction(deviceId, payload);
                break;
            case "MeterValues":
                responsePayload = handleMeterValues(deviceId, payload);
                break;
            case "Heartbeat":
                responsePayload = handleHeartbeat();
                break;
            case "StatusNotification":
                responsePayload = handleStatusNotification(deviceId, payload);
                break;
            default:
                responsePayload = createErrorPayload("NotSupported", "Action not supported");
        }

        // Create CALLRESULT response
        String response = createCallResult(messageId, responsePayload);

        // Log response
        logMessage(deviceId, messageId, action, 3, responsePayload.toString(), "OUTGOING");

        return response;
    }

    /**
     * 1. BootNotification Handler
     */
    private ObjectNode handleBootNotification(String deviceId, JsonNode payload) {
        ObjectNode response = objectMapper.createObjectNode();

        try {
            // Update device information
            SmartPlug plug = smartPlugRepository.findById(deviceId)
                    .orElseGet(() -> {
                        SmartPlug newPlug = new SmartPlug();
                        newPlug.setIdDevice(deviceId);
                        return newPlug;
                    });

            plug.setChargePointModel(payload.path("chargePointModel").asText("Unknown"));
            plug.setChargePointVendor(payload.path("chargePointVendor").asText("Unknown"));
            plug.setFirmwareVersion(payload.path("firmwareVersion").asText("1.0.0"));

            smartPlugRepository.save(plug);

            // Build OCPP response
            response.put("status", "Accepted");
            response.put("currentTime", ZonedDateTime.now(ZoneOffset.UTC).toString());
            response.put("interval", 300); // Heartbeat interval in seconds

        } catch (Exception e) {
            response.put("status", "Rejected");
        }

        return response;
    }

    /**
     * 2. Authorize Handler – uses IdDevice field
     * Returns the persistent IdTag from id_tag_info if a valid (non‑expired) record exists.
     * Does NOT create a new record – that is done only during QR scan.
     */
    private ObjectNode handleAuthorize(String deviceId, JsonNode payload) {
        ObjectNode response = objectMapper.createObjectNode();
        ObjectNode idTagInfo = objectMapper.createObjectNode();

        try {
            String deviceIdFromPayload = payload.path("IdDevice").asText();
            if (deviceIdFromPayload == null || deviceIdFromPayload.isEmpty()) {
                idTagInfo.put("status", "Invalid");
                System.err.println("❌ Authorize: missing IdDevice in payload");
            } else if (!deviceId.equals(deviceIdFromPayload)) {
                idTagInfo.put("status", "Invalid");
                System.err.println("❌ Authorize: deviceId mismatch (header=" + deviceId + ", payload=" + deviceIdFromPayload + ")");
            } else {
                LocalDateTime now = LocalDateTime.now();
                var tagOpt = idTagInfoRepository.findTopByIdDeviceOrderByCreatedAtDesc(deviceIdFromPayload);

                if (tagOpt.isPresent()) {
                    IdTagInfo tag = tagOpt.get();
                    if (tag.getExpiryDate().isAfter(now)) {
                        // Valid record found
                        idTagInfo.put("status", "Accepted");
                        idTagInfo.put("expiryDate", tag.getExpiryDate().atZone(ZoneOffset.UTC).toString());
                        idTagInfo.put("IdTag", tag.getIdTag());
                        System.out.println("✅ Authorize: returning idTag " + tag.getIdTag() + " for device " + deviceId);
                    } else {
                        // Record expired
                        idTagInfo.put("status", "Invalid");
                        System.out.println("⚠️ Authorize: latest IdTagInfo expired for device " + deviceId);
                    }
                } else {
                    // No record at all
                    idTagInfo.put("status", "Invalid");
                    System.out.println("❌ Authorize: no IdTagInfo found for device " + deviceId);
                }
            }
        } catch (Exception e) {
            idTagInfo.put("status", "Invalid");
            System.err.println("❌ Authorize error for device " + deviceId + ": " + e.getMessage());
            e.printStackTrace();
        }

        response.set("idTagInfo", idTagInfo);
        return response;
    }

    /**
     * 3. StartTransaction Handler
     */
    private ObjectNode handleStartTransaction(String deviceId, JsonNode payload) {
        ObjectNode response = objectMapper.createObjectNode();
        ObjectNode idTagInfo = objectMapper.createObjectNode();

        try {
            String idTag = payload.path("idTag").asText();
            int connectorId = payload.path("connectorId").asInt(1);
            long meterStart = payload.path("meterStart").asLong(0);

            // Validate idTag
            var tagOpt = idTagInfoRepository.findByIdTagAndIdDevice(idTag, deviceId);
            if (tagOpt.isEmpty() || !"Accepted".equals(tagOpt.get().getStatus())) {
                idTagInfo.put("status", "Invalid");
                response.set("idTagInfo", idTagInfo);
                return response;
            }

            // Check for existing active session
            if (chargingSessionRepository.findByIdDeviceAndEndTimeIsNull(deviceId).isPresent()) {
                idTagInfo.put("status", "ConcurrentTx");
                response.set("idTagInfo", idTagInfo);
                return response;
            }

            // Retrieve EV owner account number using the idTag
            String evOwnerAccountNo = null;
            Optional<EvOwner> ownerOpt = evOwnerRepository.findByIdTag(idTag);
            if (ownerOpt.isPresent()) {
                evOwnerAccountNo = ownerOpt.get().getEAccountNumber();
                System.out.println("💾 [OCPP] Found owner account " + evOwnerAccountNo + " for idTag " + idTag);
            } else {
                System.out.println("⚠️ [OCPP] No EV owner found for idTag " + idTag);
            }

            // Create new charging session
            ChargingSession session = new ChargingSession();
            session.setIdDevice(deviceId);
            session.setStartTime(LocalDateTime.now());
            session.setChargingMode("FAST");
            session.setTotalConsumption(0.0);
            session.setAmount(0.0);
            session.setSoc(0.0);
            session.setEvOwnerAccountNo(evOwnerAccountNo);

            ChargingSession savedSession = chargingSessionRepository.save(session);

            idTagInfo.put("status", "Accepted");
            response.set("idTagInfo", idTagInfo);
            response.put("transactionId", savedSession.getSessionId());

            // Send WebSocket message to frontend
            Map<String, Object> frontendMessage = new HashMap<>();
            frontendMessage.put("type", "TRANSACTION_STARTED");
            frontendMessage.put("transactionId", savedSession.getSessionId());
            frontendMessage.put("idDevice", deviceId);
            frontendMessage.put("timestamp", LocalDateTime.now().toString());

            messagingTemplate.convertAndSend("/topic/device/" + deviceId, frontendMessage);
            System.out.println("✅ Sent TRANSACTION_STARTED to frontend: " + frontendMessage);

        } catch (Exception e) {
            idTagInfo.put("status", "InternalError");
            response.set("idTagInfo", idTagInfo);
            e.printStackTrace();
        }

        return response;
    }

    /**
     * 4. MeterValues Handler
     */
    private ObjectNode handleMeterValues(String deviceId, JsonNode payload) {
        ObjectNode response = objectMapper.createObjectNode();

        try {
            int connectorId = payload.path("connectorId").asInt(1);
            Integer transactionId = payload.path("transactionId").asInt();
            JsonNode meterValueArray = payload.path("meterValue");

            System.out.println("📊 [DEBUG] Processing MeterValues for device: " + deviceId);
            System.out.println("📊 [DEBUG] Transaction ID: " + transactionId);

            if (transactionId == null) {
                System.out.println("❌ [DEBUG] No transaction ID in MeterValues");
                response.put("status", "Rejected");
                return response;
            }

            // Get the charging session
            var sessionOpt = chargingSessionRepository.findById(transactionId);
            if (sessionOpt.isEmpty()) {
                System.out.println("❌ [DEBUG] Session not found for transaction: " + transactionId);
                response.put("status", "Rejected");
                return response;
            }

            ChargingSession session = sessionOpt.get();

            // Process each meter value
            if (meterValueArray.isArray()) {
                for (JsonNode meterValue : meterValueArray) {
                    String timestampStr = meterValue.path("timestamp").asText();
                    JsonNode sampledValues = meterValue.path("sampledValue");

                    System.out.println("📊 [DEBUG] Processing timestamp: " + timestampStr);

                    if (sampledValues.isArray()) {
                        for (JsonNode sample : sampledValues) {
                            String measurand = sample.path("measurand").asText("Energy.Active.Import.Register");
                            String value = sample.path("value").asText();
                            String unit = sample.path("unit").asText("kWh");

                            System.out.println("📊 [DEBUG] Sample - Measurand: " + measurand + ", Value: " + value + ", Unit: " + unit);

                            // Extract numeric value
                            double numericValue = 0.0;
                            try {
                                numericValue = Double.parseDouble(value);
                            } catch (NumberFormatException e) {
                                System.err.println("❌ [DEBUG] Invalid value format: " + value);
                                continue;
                            }

                            // Send real-time data to frontend via WebSocket
                            Map<String, Object> realtimeData = new HashMap<>();
                            realtimeData.put("type", "METER_VALUES");
                            realtimeData.put("idDevice", deviceId);
                            realtimeData.put("transactionId", transactionId);
                            realtimeData.put("timestamp", timestampStr);

                            // Add specific measurements
                            if ("Energy.Active.Import.Register".equals(measurand)) {
                                realtimeData.put("energy", numericValue);
                                realtimeData.put("totalConsumption", numericValue);

                                // Update session total consumption
                                session.setTotalConsumption(numericValue);
                                chargingSessionRepository.save(session);

                            } else if ("Power.Active.Import".equals(measurand) || "Power.Active.Import.Register".equals(measurand)) {
                                realtimeData.put("power", numericValue);
                            } else if ("Voltage".equals(measurand)) {
                                realtimeData.put("voltage", numericValue);
                            } else if ("Current.Import".equals(measurand)) {
                                realtimeData.put("current", numericValue);
                            } else if ("Temperature".equals(measurand)) {
                                realtimeData.put("temperature", numericValue);
                            }

                            // Send to frontend
                            sendMeterValueToFrontend(realtimeData);
                        }
                    }
                }
            }

            return objectMapper.createObjectNode();

        } catch (Exception e) {
            ObjectNode errorResponse = objectMapper.createObjectNode();
            errorResponse.put("status", "Rejected");
            return errorResponse;
        }

    }

    /**
     * Send meter values to frontend via STOMP
     */
    private void sendMeterValueToFrontend(Map<String, Object> meterData) {
        try {
            String deviceId = (String) meterData.get("idDevice");

            if (deviceId == null) {
                System.err.println("❌ [DEBUG] Cannot send meter data: deviceId is null");
                return;
            }

            // Create a properly formatted message for frontend
            Map<String, Object> frontendMessage = new HashMap<>();
            frontendMessage.put("type", "METER_VALUES");
            frontendMessage.put("idDevice", deviceId);
            frontendMessage.put("timestamp", meterData.get("timestamp"));

            // Add sampled values in the format frontend expects
            List<Map<String, Object>> sampledValues = new ArrayList<>();

            // Add power data if available
            if (meterData.containsKey("power")) {
                Map<String, Object> powerSample = new HashMap<>();
                powerSample.put("value", meterData.get("power").toString());
                powerSample.put("measurand", "Power.Active.Import");
                powerSample.put("unit", "kW");
                sampledValues.add(powerSample);
            }

            // Add voltage data if available
            if (meterData.containsKey("voltage")) {
                Map<String, Object> voltageSample = new HashMap<>();
                voltageSample.put("value", meterData.get("voltage").toString());
                voltageSample.put("measurand", "Voltage");
                voltageSample.put("unit", "V");
                sampledValues.add(voltageSample);
            }

            // Add current data if available
            if (meterData.containsKey("current")) {
                Map<String, Object> currentSample = new HashMap<>();
                currentSample.put("value", meterData.get("current").toString());
                currentSample.put("measurand", "Current.Import");
                currentSample.put("unit", "A");
                sampledValues.add(currentSample);
            }

            // Add energy data if available
            if (meterData.containsKey("energy")) {
                Map<String, Object> energySample = new HashMap<>();
                energySample.put("value", meterData.get("energy").toString());
                energySample.put("measurand", "Energy.Active.Import.Register");
                energySample.put("unit", "kWh");
                sampledValues.add(energySample);
            }

            // Create meterValue object
            Map<String, Object> meterValueObj = new HashMap<>();
            meterValueObj.put("sampledValue", sampledValues);
            frontendMessage.put("meterValue", meterValueObj);

            System.out.println("📤 [DEBUG] Sending to frontend device " + deviceId + ": " + frontendMessage);

            // Send to device-specific topic
            messagingTemplate.convertAndSend("/topic/device/" + deviceId, frontendMessage);

            // Also send to general charging topic
            messagingTemplate.convertAndSend("/topic/charging", frontendMessage);

        } catch (Exception e) {
            System.err.println("❌ [DEBUG] Error sending meter data to frontend: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 5. StopTransaction Handler
     */
    private ObjectNode handleStopTransaction(String deviceId, JsonNode payload) {
        ObjectNode response = objectMapper.createObjectNode();
        ObjectNode idTagInfo = objectMapper.createObjectNode();

        try {
            Integer transactionId = payload.path("transactionId").asInt();
            Long meterStop = payload.path("meterStop").asLong();
            String timestampStr = payload.path("timestamp").asText();

            // Get session
            var sessionOpt = chargingSessionRepository.findById(transactionId);
            if (sessionOpt.isEmpty()) {
                idTagInfo.put("status", "Invalid");
                response.set("idTagInfo", idTagInfo);
                return response;
            }

            ChargingSession session = sessionOpt.get();

            // Verify device ownership
            if (!deviceId.equals(session.getIdDevice())) {
                idTagInfo.put("status", "Invalid");
                response.set("idTagInfo", idTagInfo);
                return response;
            }

            // USE SERVER TIME instead of device timestamp
            LocalDateTime endTime = LocalDateTime.now();
            System.out.println("⏰ [DEBUG] Using server end time: " + endTime);
            System.out.println("⏰ [DEBUG] Session start time: " + session.getStartTime());

            // Calculate duration in minutes
            long durationMinutes = 0;
            if (session.getStartTime() != null) {
                durationMinutes = java.time.Duration.between(session.getStartTime(), endTime).toMinutes();
            }

            // Calculate cost at $0.15 per kWh
            double cost = 0.0;
            if (meterStop != null) {
                double consumptionKWh = meterStop.doubleValue();
                cost = consumptionKWh * 0.15; // $0.15 per kWh

                // Update session with final values
                session.setTotalConsumption(consumptionKWh);
                session.setAmount(cost);
                session.setEndTime(endTime);

                chargingSessionRepository.save(session);

                System.out.println("💰 Transaction completed:");
                System.out.println("   - Consumption: " + consumptionKWh + " kWh");
                System.out.println("   - Duration: " + durationMinutes + " minutes");
                System.out.println("   - Cost: $" + String.format("%.2f", cost));

                // Send transaction details to frontend
                Map<String, Object> transactionDetails = new HashMap<>();
                transactionDetails.put("type", "TRANSACTION_COMPLETED");
                transactionDetails.put("transactionId", transactionId);
                transactionDetails.put("idDevice", deviceId);
                transactionDetails.put("powerConsumed", consumptionKWh);
                transactionDetails.put("durationMinutes", durationMinutes);
                transactionDetails.put("cost", String.format("%.2f", cost));
                transactionDetails.put("startTime", session.getStartTime().toString());
                transactionDetails.put("endTime", endTime.toString());
                transactionDetails.put("timestamp", LocalDateTime.now().toString());

                messagingTemplate.convertAndSend("/topic/device/" + deviceId, transactionDetails);
                messagingTemplate.convertAndSend("/topic/charging", transactionDetails);
            }

            idTagInfo.put("status", "Accepted");
            response.set("idTagInfo", idTagInfo);

        } catch (Exception e) {
            idTagInfo.put("status", "InternalError");
            response.set("idTagInfo", idTagInfo);
            System.err.println("❌ Error in handleStopTransaction: " + e.getMessage());
        }

        return response;

    }

    /**
     * Heartbeat Handler – return current time in UTC with Z
     */
    private ObjectNode handleHeartbeat() {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("currentTime", ZonedDateTime.now(ZoneOffset.UTC).toString());
        return response;
    }

    /**
     * StatusNotification Handler – persist device status
     */
    private ObjectNode handleStatusNotification(String deviceId, JsonNode payload) {
        ObjectNode response = objectMapper.createObjectNode();
        try {
            String status = payload.path("status").asText();
            // Update smart_plug status
            smartPlugRepository.findById(deviceId).ifPresent(plug -> {
                plug.setStatus(status);
                smartPlugRepository.save(plug);
            });
            response.put("status", "Accepted");
        } catch (Exception e) {
            response.put("status", "Rejected");
        }
        return response;
    }

    private String handleCallResult(String deviceId, String messageId, JsonNode payload) {
        // Handle responses to our calls (like RemoteStartTransaction.conf)
        // You can implement callback logic here
        return null;
    }

    private String handleCallError(String deviceId, String messageId, JsonNode payload) {
        // Handle error responses
        return null;
    }

    private String createCallResult(String messageId, JsonNode payload) {
        ArrayNode response = objectMapper.createArrayNode();
        response.add(3); // MessageTypeId for CALLRESULT
        response.add(messageId);
        response.add(payload);
        return response.toString();
    }

    private String createError(String errorCode, String errorDescription, String messageId) {
        ArrayNode response = objectMapper.createArrayNode();
        response.add(4); // MessageTypeId for CALLERROR
        response.add(messageId != null ? messageId : "");
        response.add(errorCode);
        response.add(errorDescription);
        response.add(objectMapper.createObjectNode()); // Empty error details
        return response.toString();
    }

    private ObjectNode createErrorPayload(String status, String message) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("status", status);
        if (message != null) {
            payload.put("message", message);
        }
        return payload;
    }

    private void logMessage(String deviceId, String messageId, String action,
                            int messageTypeId, String payload, String direction) {
        try {
            OcppMessageLog log = new OcppMessageLog();
            log.setIdDevice(deviceId);
            log.setMessageId(messageId);
            log.setAction(action);
            log.setMessageTypeId(messageTypeId);
            log.setPayload(payload);
            log.setReceivedAt(LocalDateTime.now());
            messageLogRepository.save(log);
        } catch (Exception e) {
            // Silent fail on logging errors
        }
    }

}
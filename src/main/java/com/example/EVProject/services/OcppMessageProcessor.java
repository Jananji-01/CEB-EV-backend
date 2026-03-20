// package com.example.EVProject.services;

// import com.example.EVProject.model.*;
// import com.example.EVProject.repositories.*;
// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.fasterxml.jackson.databind.node.ArrayNode;
// import com.fasterxml.jackson.databind.node.ObjectNode;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.context.annotation.Lazy;
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
//     private MeterValueRepository meterValueRepository;
//     @Autowired
//     private SampledValueRepository sampledValueRepository;
//     @Autowired
//     private OcppMessageLogRepository messageLogRepository;
//     @Autowired
//     private SimpMessagingTemplate messagingTemplate;
//     @Lazy
//     @Autowired
//     private OcppWebSocketService ocppWebSocketService;

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
//         ObjectNode responsePayload;

//         switch (action) {
//             case "BootNotification":
//                 responsePayload = handleBootNotification(deviceId, payload);
//                 break;
//             case "Authorize":
//                 responsePayload = handleAuthorize(deviceId, payload);
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
//      * 1. BootNotification Handler
//      */
//     private ObjectNode handleBootNotification(String deviceId, JsonNode payload) {
//         ObjectNode response = objectMapper.createObjectNode();

//         try {
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
//      */
//     private ObjectNode handleAuthorize(String deviceId, JsonNode payload) {
//         ObjectNode response = objectMapper.createObjectNode();
//         ObjectNode idTagInfo = objectMapper.createObjectNode();

//         try {
//             String deviceIdFromPayload = payload.path("IdDevice").asText();
//             if (deviceIdFromPayload == null || deviceIdFromPayload.isEmpty()) {
//                 idTagInfo.put("status", "Invalid");
//                 System.err.println("❌ Authorize: missing IdDevice in payload");
//             } else if (!deviceId.equals(deviceIdFromPayload)) {
//                 idTagInfo.put("status", "Invalid");
//                 System.err.println("❌ Authorize: deviceId mismatch (header=" + deviceId + ", payload=" + deviceIdFromPayload + ")");
//             } else {
//                 LocalDateTime now = LocalDateTime.now();
//                 IdTagInfo tag = null;

//                 // 1. Try to get the latest IdTagInfo for this device
//                 var tagOpt = idTagInfoRepository.findTopByIdDeviceOrderByCreatedAtDesc(deviceIdFromPayload);
//                 if (tagOpt.isPresent()) {
//                     tag = tagOpt.get();
//                     System.out.println("✅ Authorize: found latest IdTagInfo for device " + deviceId +
//                             ": id=" + tag.getId() + ", tag=" + tag.getIdTag() +
//                             ", expiry=" + tag.getExpiryDate() + ", status=" + tag.getStatus());

//                     // 2. If it's expired, we'll discard it and create a new one
//                     if (tag.getExpiryDate().isBefore(now)) {
//                         System.out.println("⚠️ Authorize: latest tag expired, generating new one");
//                         tag = null;  // will create new below
//                     }
//                 } else {
//                     System.out.println("✅ Authorize: no existing tag, creating new one");
//                 }

//                 // 3. If no valid tag exists, create a new one
//                 if (tag == null) {
//                     tag = new IdTagInfo();
//                     tag.setIdTag(generateIdTag(deviceIdFromPayload));
//                     tag.setIdDevice(deviceIdFromPayload);
//                     tag.setStatus("Accepted");
//                     tag.setExpiryDate(now.plusHours(2));  // 2 hours validity
//                     tag.setCreatedAt(now);
//                     idTagInfoRepository.save(tag);
//                     System.out.println("✅ Authorize: created new IdTagInfo for device " + deviceId + ": " + tag.getIdTag());
//                 }

//                 // 4. Return the tag info (should always be accepted now)
//                 idTagInfo.put("status", "Accepted");
//                 idTagInfo.put("expiryDate", tag.getExpiryDate().atZone(ZoneOffset.UTC).toString());
//                 idTagInfo.put("IdTag", tag.getIdTag());
//             }
//         } catch (Exception e) {
//             idTagInfo.put("status", "Invalid");
//             System.err.println("❌ Authorize error for device " + deviceId + ": " + e.getMessage());
//             e.printStackTrace();
//         }

//         response.set("idTagInfo", idTagInfo);
//         return response;
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

//             // Create new charging session
//             ChargingSession session = new ChargingSession();
//             session.setIdDevice(deviceId);
//             session.setStartTime(LocalDateTime.now());
//             session.setChargingMode("FAST");
//             session.setTotalConsumption(0.0);
//             session.setAmount(0.0);
//             session.setSoc(0.0);

//             String evOwnerAccountNo = ocppWebSocketService.retrieveEvOwnerAccountByDeviceId(deviceId);
//             if (evOwnerAccountNo != null) {
//                 session.setEvOwnerAccountNo(evOwnerAccountNo);
//                 System.out.println("💾 [OCPP] Set EV owner account for session: " + evOwnerAccountNo);
//             } else {
//                 System.out.println("⚠️ [OCPP] No EV owner account found for device: " + deviceId);
//             }

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
//      * Generate a unique IdTag for a device
//      */
//     private String generateIdTag(String base) {
//         return "IDT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
//     }
// }

// package com.example.EVProject.services;

// import com.example.EVProject.model.*;
// import com.example.EVProject.repositories.*;
// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.fasterxml.jackson.databind.node.ArrayNode;
// import com.fasterxml.jackson.databind.node.ObjectNode;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;

// import java.time.LocalDateTime;
// import java.time.ZoneOffset;
// import java.time.ZonedDateTime;
// import java.time.format.DateTimeFormatter;
// import java.util.*;

// @Service
// public class OcppMessageProcessor {

//     @Autowired
//     private OcppMessageLogRepository messageLogRepository;

//     private final ObjectMapper objectMapper = new ObjectMapper();

//     /**
//      * Process incoming OCPP WebSocket message (minimal: only Heartbeat)
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

//         private String handleCall(String deviceId, String messageId, String action, JsonNode payload) {
//         ObjectNode responsePayload;

//         switch (action) {
//             case "Heartbeat":
//                 responsePayload = handleHeartbeat();
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
//      * Heartbeat Handler – returns current UTC time
//      */
//     private ObjectNode handleHeartbeat() {
//         ObjectNode response = objectMapper.createObjectNode();
//         ZoneOffset offset = ZoneOffset.ofHoursMinutes(5, 30);
//         response.put("currentTime", ZonedDateTime.now(offset).toString());
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
// }

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
//         ObjectNode responsePayload;

//         switch (action) {
//             case "BootNotification":
//                 responsePayload = handleBootNotification(deviceId, payload);
//                 break;
//             case "Authorize":
//                 responsePayload = handleAuthorize(deviceId, payload);
//                 break;
//             case "StartTransaction":
//                 responsePayload = handleStartTransaction(deviceId, messageId, payload);
//                 break;
//             case "StopTransaction":
//                 responsePayload = handleStopTransaction(deviceId, messageId, payload);
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

//             plug.setChargePointModel(model);
//             plug.setChargePointVendor(vendor);
//             plug.setFirmwareVersion(firmware);

//             smartPlugRepository.save(plug);

//             // Build OCPP response matching REST API format
//             response.put("status", "Accepted");
//             response.put("currentTime", LocalDateTime.now(ZoneOffset.UTC).toString());
//             response.put("interval", 300); // Heartbeat interval in seconds

//         } catch (Exception e) {
//             response.put("status", "Rejected");
//         }

//         return response;
//     }

//     /**
//      * 2. Authorize Handler - Matches REST API format
//      */
//     private ObjectNode handleAuthorize(String deviceId, JsonNode payload) {
//         ObjectNode response = objectMapper.createObjectNode();
//         ObjectNode idTagInfo = objectMapper.createObjectNode();

//         try {
//             String deviceIdFromPayload = payload.path("IdDevice").asText();
            
//             // Validate device ID
//             if (deviceIdFromPayload == null || deviceIdFromPayload.isEmpty()) {
//                 idTagInfo.put("status", "Invalid");
//                 System.err.println("❌ Authorize: missing IdDevice in payload");
//             } else if (!deviceId.equals(deviceIdFromPayload)) {
//                 idTagInfo.put("status", "Invalid");
//                 System.err.println("❌ Authorize: deviceId mismatch (header=" + deviceId + ", payload=" + deviceIdFromPayload + ")");
//             } else {
//                 LocalDateTime now = LocalDateTime.now();
//                 IdTagInfo tag = null;

//                 // Check for existing valid tag
//                 List<IdTagInfo> existingTags = idTagInfoRepository.findByIdDevice(deviceIdFromPayload);
                
//                 for (IdTagInfo existingTag : existingTags) {
//                     if (existingTag.getExpiryDate().isAfter(now)) {
//                         tag = existingTag;
//                         break;
//                     }
//                 }

//                 if (tag != null) {
//                     // Reuse valid tag
//                     System.out.println("✅ Authorize: reusing valid tag for device " + deviceId + ": " + tag.getIdTag());
//                 } else {
//                     // Generate new tag
//                     SmartPlug plug = smartPlugRepository.findById(deviceIdFromPayload)
//                             .orElseThrow(() -> new IllegalArgumentException("IdDevice not found: " + deviceIdFromPayload));

//                     String accountReference = (plug.getCebSerialNo() != null)
//                             ? plug.getCebSerialNo()
//                             : plug.getIdDevice();

//                     String idTag = generateIdTag(accountReference);
//                     LocalDateTime expiryDate = now.plusHours(6);

//                     tag = new IdTagInfo();
//                     tag.setIdDevice(deviceIdFromPayload);
//                     tag.setIdTag(idTag);
//                     tag.setStatus("Accepted");
//                     tag.setExpiryDate(expiryDate);
//                     tag.setCreatedAt(now);
//                     idTagInfoRepository.save(tag);
                    
//                     System.out.println("✅ Authorize: created new IdTagInfo for device " + deviceId + ": " + idTag);
//                 }

//                 // Return in same format as REST API
//                 idTagInfo.put("status", "Accepted");
//                 idTagInfo.put("expiryDate", tag.getExpiryDate().atZone(ZoneOffset.UTC).toString());
//                 idTagInfo.put("IdTag", tag.getIdTag());
//             }
//         } catch (Exception e) {
//             idTagInfo.put("status", "Invalid");
//             System.err.println("❌ Authorize error for device " + deviceId + ": " + e.getMessage());
//             e.printStackTrace();
//         }

//         response.set("idTagInfo", idTagInfo);
//         return response;
//     }

//     /**
//      * 3. StartTransaction Handler - Matches REST API format (returns only status)
//      */
//     private ObjectNode handleStartTransaction(String deviceId, String messageId, JsonNode payload) {
//         ObjectNode response = objectMapper.createObjectNode();
//         ObjectNode idTagInfo = objectMapper.createObjectNode();

//         try {
//             String idTag = payload.path("idTag").asText();
//             int connectorId = payload.path("connectorId").asInt(1);
//             long meterStart = payload.path("meterStart").asLong(0);

//             // Validate idTag
//             Optional<IdTagInfo> tagOpt = idTagInfoRepository.findByIdTag(idTag);
            
//             if (tagOpt.isEmpty()) {
//                 idTagInfo.put("status", "Invalid");
//                 response.set("idTagInfo", idTagInfo);
//                 return response;
//             }

//             IdTagInfo tag = tagOpt.get();
            
//             // Check if tag is expired
//             if (tag.getExpiryDate().isBefore(LocalDateTime.now())) {
//                 idTagInfo.put("status", "Expired");
//                 response.set("idTagInfo", idTagInfo);
//                 return response;
//             }
            
//             // Check if tag is accepted
//             if (!"Accepted".equals(tag.getStatus())) {
//                 idTagInfo.put("status", tag.getStatus());
//                 response.set("idTagInfo", idTagInfo);
//                 return response;
//             }

//             // Check if tag belongs to this device
//             if (!deviceId.equals(tag.getIdDevice())) {
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

//             // Create new charging session
//             ChargingSession session = new ChargingSession();
//             session.setIdDevice(deviceId);
//             session.setStartTime(LocalDateTime.now());
//             session.setChargingMode("FAST");
//             session.setTotalConsumption(0.0);
//             session.setAmount(0.0);
//             session.setSoc(0.0);


//             ChargingSession savedSession = chargingSessionRepository.save(session);

//             // Return ONLY status in idTagInfo (no transactionId in response)
//             idTagInfo.put("status", "Accepted");
//             response.set("idTagInfo", idTagInfo);
            
//             // Note: transactionId is NOT returned in the OCPP response
//             // It's only used internally and for frontend notifications

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
//         // Return empty object as in REST API
//         ObjectNode response = objectMapper.createObjectNode();

//         try {
//             int connectorId = payload.path("connectorId").asInt(1);
//             Integer transactionId = payload.path("transactionId").asInt();
//             JsonNode meterValueArray = payload.path("meterValue");

//             System.out.println("📊 [DEBUG] Processing MeterValues for device: " + deviceId);
//             System.out.println("📊 [DEBUG] Transaction ID: " + transactionId);

//             if (transactionId == null) {
//                 System.out.println("❌ [DEBUG] No transaction ID in MeterValues");
//                 return response; // Return empty, not error
//             }

//             // Get the charging session
//             var sessionOpt = chargingSessionRepository.findById(transactionId);
//             if (sessionOpt.isEmpty()) {
//                 System.out.println("❌ [DEBUG] Session not found for transaction: " + transactionId);
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

//                             if ("Energy.Active.Import.Register".equals(measurand)) {
//                                 realtimeData.put("energy", numericValue);
//                                 realtimeData.put("totalConsumption", numericValue);
//                                 session.setTotalConsumption(numericValue);
//                                 chargingSessionRepository.save(session);
//                             } else if ("Power.Active.Import".equals(measurand)) {
//                                 realtimeData.put("power", numericValue);
//                             } else if ("Voltage".equals(measurand)) {
//                                 realtimeData.put("voltage", numericValue);
//                             } else if ("Current.Import".equals(measurand)) {
//                                 realtimeData.put("current", numericValue);
//                             }

//                             sendMeterValueToFrontend(realtimeData);
//                         }
//                     }
//                 }
//             }

//             return response; // Return empty object

//         } catch (Exception e) {
//             System.err.println("Error in MeterValues: " + e.getMessage());
//             return objectMapper.createObjectNode(); // Return empty on error too
//         }
//     }

//     /**
//      * 5. StopTransaction Handler - Matches REST API format
//      */
//     private ObjectNode handleStopTransaction(String deviceId, String messageId, JsonNode payload) {
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

//             // Validate IdTag if provided
//             if (idTag != null && !idTag.isEmpty()) {
//                 var tagOpt = idTagInfoRepository.findByIdTagAndIdDevice(idTag, deviceId);
                
//                 if (tagOpt.isEmpty()) {
//                     idTagInfo.put("status", "Invalid");
//                     response.set("idTagInfo", idTagInfo);
//                     return response;
//                 }

//                 var tag = tagOpt.get();
//                 if (!"Accepted".equals(tag.getStatus())) {
//                     idTagInfo.put("status", tag.getStatus());
//                     response.set("idTagInfo", idTagInfo);
//                     return response;
//                 }
                
//                 if (tag.getExpiryDate().isBefore(LocalDateTime.now())) {
//                     idTagInfo.put("status", "Expired");
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

//             // Use server time
//             LocalDateTime endTime = LocalDateTime.now();
//             System.out.println("⏰ [DEBUG] Using server end time: " + endTime);

//             // Calculate duration
//             long durationMinutes = 0;
//             if (session.getStartTime() != null) {
//                 durationMinutes = java.time.Duration.between(session.getStartTime(), endTime).toMinutes();
//             }

//             // Calculate cost
//             double cost = 0.0;
//             if (meterStop != null) {
//                 double consumptionKWh = meterStop.doubleValue();
//                 cost = consumptionKWh * 0.15;

//                 session.setTotalConsumption(consumptionKWh);
//                 session.setAmount(cost);
//                 session.setEndTime(endTime);
//                 chargingSessionRepository.save(session);

//                 System.out.println("💰 Transaction completed:");
//                 System.out.println("   - Consumption: " + consumptionKWh + " kWh");
//                 System.out.println("   - Duration: " + durationMinutes + " minutes");
//                 System.out.println("   - Cost: $" + String.format("%.2f", cost));
//             }

//             // Return only idTagInfo with status (matches REST API)
//             idTagInfo.put("status", "Accepted");
//             response.set("idTagInfo", idTagInfo);

//             // Send transaction details to frontend
//             Map<String, Object> transactionDetails = new HashMap<>();
//             transactionDetails.put("type", "TRANSACTION_COMPLETED");
//             transactionDetails.put("transactionId", transactionId);
//             transactionDetails.put("idDevice", deviceId);
//             transactionDetails.put("powerConsumed", session.getTotalConsumption());
//             transactionDetails.put("durationMinutes", durationMinutes);
//             transactionDetails.put("cost", String.format("%.2f", cost));
//             transactionDetails.put("startTime", session.getStartTime().toString());
//             transactionDetails.put("endTime", endTime.toString());
//             transactionDetails.put("timestamp", LocalDateTime.now().toString());

//             messagingTemplate.convertAndSend("/topic/device/" + deviceId, transactionDetails);
//             messagingTemplate.convertAndSend("/topic/charging", transactionDetails);

//         } catch (Exception e) {
//             idTagInfo.put("status", "InternalError");
//             response.set("idTagInfo", idTagInfo);
//             System.err.println("❌ Error in handleStopTransaction: " + e.getMessage());
//             e.printStackTrace();
//         }

//         return response;
//     }

//     /**
//      * Heartbeat Handler – matches REST API format
//      */
//     private ObjectNode handleHeartbeat() {
//         ObjectNode response = objectMapper.createObjectNode();
//         response.put("currentTime", LocalDateTime.now(ZoneOffset.UTC).toString());
//         return response;
//     }

//     /**
//      * StatusNotification Handler – matches REST API format (empty response)
//      */
//     private ObjectNode handleStatusNotification(String deviceId, JsonNode payload) {
//         ObjectNode response = objectMapper.createObjectNode(); // Empty response
//         try {
//             String status = payload.path("status").asText();
//             String errorCode = payload.path("errorCode").asText();
            
//             // Update smart_plug status
//             smartPlugRepository.findById(deviceId).ifPresent(plug -> {
//                 plug.setStatus(status);
//                 smartPlugRepository.save(plug);
//             });
            
//             // Return empty object as in REST API
//         } catch (Exception e) {
//             System.err.println("Error in StatusNotification: " + e.getMessage());
//         }
//         return response; // Empty object
//     }

//     private String handleCallResult(String deviceId, String messageId, JsonNode payload) {
//         // Handle responses to our calls
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
//         response.add(objectMapper.createObjectNode());
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
//      * Generate a unique IdTag for a device
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
//      * Send meter values to frontend via STOMP
//      */
//     private void sendMeterValueToFrontend(Map<String, Object> meterData) {
//         try {
//             String deviceId = (String) meterData.get("idDevice");
//             if (deviceId == null) {
//                 System.err.println("❌ Cannot send meter data: deviceId is null");
//                 return;
//             }

//             Map<String, Object> frontendMessage = new HashMap<>();
//             frontendMessage.put("type", "METER_VALUES");
//             frontendMessage.put("idDevice", deviceId);
//             frontendMessage.put("timestamp", meterData.get("timestamp"));

//             List<Map<String, Object>> sampledValues = new ArrayList<>();

//             if (meterData.containsKey("power")) {
//                 Map<String, Object> powerSample = new HashMap<>();
//                 powerSample.put("value", meterData.get("power").toString());
//                 powerSample.put("measurand", "Power.Active.Import");
//                 powerSample.put("unit", "kW");
//                 sampledValues.add(powerSample);
//             }

//             if (meterData.containsKey("voltage")) {
//                 Map<String, Object> voltageSample = new HashMap<>();
//                 voltageSample.put("value", meterData.get("voltage").toString());
//                 voltageSample.put("measurand", "Voltage");
//                 voltageSample.put("unit", "V");
//                 sampledValues.add(voltageSample);
//             }

//             if (meterData.containsKey("current")) {
//                 Map<String, Object> currentSample = new HashMap<>();
//                 currentSample.put("value", meterData.get("current").toString());
//                 currentSample.put("measurand", "Current.Import");
//                 currentSample.put("unit", "A");
//                 sampledValues.add(currentSample);
//             }

//             if (meterData.containsKey("energy")) {
//                 Map<String, Object> energySample = new HashMap<>();
//                 energySample.put("value", meterData.get("energy").toString());
//                 energySample.put("measurand", "Energy.Active.Import.Register");
//                 energySample.put("unit", "kWh");
//                 sampledValues.add(energySample);
//             }

//             Map<String, Object> meterValueObj = new HashMap<>();
//             meterValueObj.put("sampledValue", sampledValues);
//             frontendMessage.put("meterValue", meterValueObj);

//             messagingTemplate.convertAndSend("/topic/device/" + deviceId, frontendMessage);
//             messagingTemplate.convertAndSend("/topic/charging", frontendMessage);

//         } catch (Exception e) {
//             System.err.println("❌ Error sending meter data to frontend: " + e.getMessage());
//         }
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
    private MeterValueRepository meterValueRepository;
    @Autowired
    private SampledValueRepository sampledValueRepository;
    @Autowired
    private OcppMessageLogRepository messageLogRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

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
                responsePayload = handleStartTransaction(deviceId, messageId, payload);
                break;
            case "StopTransaction":
                responsePayload = handleStopTransaction(deviceId, messageId, payload);
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
     * 1. BootNotification Handler - Matches REST API format
     */
    private ObjectNode handleBootNotification(String deviceId, JsonNode payload) {
        ObjectNode response = objectMapper.createObjectNode();

        try {
            // Extract payload fields
            String model = payload.has("chargePointModel") ? payload.get("chargePointModel").asText() : null;
            String vendor = payload.has("chargePointVendor") ? payload.get("chargePointVendor").asText() : null;
            String firmware = payload.has("firmwareVersion") ? payload.get("firmwareVersion").asText() : null;

            // Update device information
            SmartPlug plug = smartPlugRepository.findById(deviceId)
                    .orElseGet(() -> {
                        SmartPlug newPlug = new SmartPlug();
                        newPlug.setIdDevice(deviceId);
                        return newPlug;
                    });

            plug.setChargePointModel(model);
            plug.setChargePointVendor(vendor);
            plug.setFirmwareVersion(firmware);

            smartPlugRepository.save(plug);

            // Build OCPP response matching REST API format
            response.put("status", "Accepted");
            response.put("currentTime", LocalDateTime.now().toString() + "Z");
            response.put("interval", 300); // Heartbeat interval in seconds

        } catch (Exception e) {
            response.put("status", "Rejected");
        }

        return response;
    }

    /**
     * 2. Authorize Handler - Simple version: use existing tag if exists, otherwise create new
     * No expiry checking for tag existence
     */
    private ObjectNode handleAuthorize(String deviceId, JsonNode payload) {
        ObjectNode response = objectMapper.createObjectNode();
        ObjectNode idTagInfo = objectMapper.createObjectNode();

        try {
            String deviceIdFromPayload = payload.path("IdDevice").asText();
            
            // Validate device ID
            if (deviceIdFromPayload == null || deviceIdFromPayload.isEmpty()) {
                idTagInfo.put("status", "Invalid");
                System.err.println("❌ Authorize: missing IdDevice in payload");
            } else if (!deviceId.equals(deviceIdFromPayload)) {
                idTagInfo.put("status", "Invalid");
                System.err.println("❌ Authorize: deviceId mismatch (header=" + deviceId + ", payload=" + deviceIdFromPayload + ")");
            } else {
                LocalDateTime now = LocalDateTime.now();
                IdTagInfo tag = null;

                // STEP 1: Check if device already has ANY tag in database
                List<IdTagInfo> existingTags = idTagInfoRepository.findByIdDevice(deviceIdFromPayload);
                
                if (!existingTags.isEmpty()) {
                    // Use the first existing tag (you might want to use the most recent one)
                    tag = existingTags.get(0);
                    System.out.println("✅ Authorize: Using existing tag for device " + deviceId + 
                                    ": " + tag.getIdTag());
                } else {
                    // STEP 2: No tag exists, create brand new one
                    SmartPlug plug = smartPlugRepository.findById(deviceIdFromPayload)
                            .orElseThrow(() -> new IllegalArgumentException("IdDevice not found: " + deviceIdFromPayload));

                    String accountReference = (plug.getCebSerialNo() != null)
                            ? plug.getCebSerialNo()
                            : plug.getIdDevice();

                    String idTag = generateIdTag(accountReference);
                    LocalDateTime expiryDate = now.plusHours(6); // Still set expiry even if not checked

                    tag = new IdTagInfo();
                    tag.setIdDevice(deviceIdFromPayload);
                    tag.setIdTag(idTag);
                    tag.setStatus("Accepted");
                    tag.setExpiryDate(expiryDate);
                    tag.setCreatedAt(now);
                    idTagInfoRepository.save(tag);
                    
                    System.out.println("✅ Authorize: Created NEW IdTag for device " + deviceId + 
                                    ": " + idTag);
                }

                // Return the tag info (always Accepted)
                idTagInfo.put("status", "Accepted");
                idTagInfo.put("expiryDate", tag.getExpiryDate().atZone(ZoneOffset.UTC).toString());
                idTagInfo.put("IdTag", tag.getIdTag());
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
     * 3. StartTransaction Handler - Matches REST API format with expiry validation
     */
    private ObjectNode handleStartTransaction(String deviceId, String messageId, JsonNode payload) {
        ObjectNode response = objectMapper.createObjectNode();
        ObjectNode idTagInfo = objectMapper.createObjectNode();

        try {
            String idTag = payload.path("idTag").asText();
            int connectorId = payload.path("connectorId").asInt(1);
            long meterStart = payload.path("meterStart").asLong(0);

            // Validate idTag with expiry check - same as REST API
            Optional<IdTagInfo> tagOpt = idTagInfoRepository.findByIdTag(idTag);
            LocalDateTime now = LocalDateTime.now();
            
            if (tagOpt.isEmpty()) {
                idTagInfo.put("status", "Invalid");
                response.set("idTagInfo", idTagInfo);
                return response;
            }

            IdTagInfo tag = tagOpt.get();
            
            // Check if tag is expired - same logic as REST API
            if (tag.getExpiryDate().isBefore(now)) {
                idTagInfo.put("status", "Expired");
                System.out.println("❌ StartTransaction: IdTag expired: " + idTag + 
                                 " (expired: " + tag.getExpiryDate() + ")");
                response.set("idTagInfo", idTagInfo);
                return response;
            }
            
            // Check if tag is accepted
            if (!"Accepted".equals(tag.getStatus())) {
                idTagInfo.put("status", tag.getStatus());
                System.out.println("❌ StartTransaction: IdTag status invalid: " + tag.getStatus());
                response.set("idTagInfo", idTagInfo);
                return response;
            }

            // Check if tag belongs to this device
            if (!deviceId.equals(tag.getIdDevice())) {
                idTagInfo.put("status", "Invalid");
                System.out.println("❌ StartTransaction: IdTag device mismatch");
                response.set("idTagInfo", idTagInfo);
                return response;
            }

            // Check for existing active session
            if (chargingSessionRepository.findByIdDeviceAndEndTimeIsNull(deviceId).isPresent()) {
                idTagInfo.put("status", "ConcurrentTx");
                System.out.println("❌ StartTransaction: Concurrent transaction detected");
                response.set("idTagInfo", idTagInfo);
                return response;
            }

            // Create new charging session
            ChargingSession session = new ChargingSession();
            session.setIdDevice(deviceId);
            session.setStartTime(LocalDateTime.now());
            session.setChargingMode("FAST");
            session.setTotalConsumption(0.0);
            session.setAmount(0.0);
            session.setSoc(0.0);

            ChargingSession savedSession = chargingSessionRepository.save(session);

            // Return ONLY status in idTagInfo (no transactionId in response)
            idTagInfo.put("status", "Accepted");
            response.set("idTagInfo", idTagInfo);
            
            System.out.println("✅ StartTransaction: Started for device " + deviceId + 
                             " with tag " + idTag + " (expires: " + tag.getExpiryDate() + ")");

            // Send WebSocket message to frontend
            Map<String, Object> frontendMessage = new HashMap<>();
            frontendMessage.put("type", "TRANSACTION_STARTED");
            frontendMessage.put("transactionId", savedSession.getSessionId());
            frontendMessage.put("idDevice", deviceId);
            frontendMessage.put("idTag", idTag);
            frontendMessage.put("idTagExpiry", tag.getExpiryDate().toString());
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
        // Return empty object as in REST API
        ObjectNode response = objectMapper.createObjectNode();

        try {
            int connectorId = payload.path("connectorId").asInt(1);
            Integer transactionId = payload.path("transactionId").asInt();
            JsonNode meterValueArray = payload.path("meterValue");

            System.out.println("📊 [DEBUG] Processing MeterValues for device: " + deviceId);
            System.out.println("📊 [DEBUG] Transaction ID: " + transactionId);

            if (transactionId == null) {
                System.out.println("❌ [DEBUG] No transaction ID in MeterValues");
                return response; // Return empty, not error
            }

            // Get the charging session
            var sessionOpt = chargingSessionRepository.findById(transactionId);
            if (sessionOpt.isEmpty()) {
                System.out.println("❌ [DEBUG] Session not found for transaction: " + transactionId);
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

                            if ("Energy.Active.Import.Register".equals(measurand)) {
                                realtimeData.put("energy", numericValue);
                                realtimeData.put("totalConsumption", numericValue);
                                session.setTotalConsumption(numericValue);
                                chargingSessionRepository.save(session);
                            } else if ("Power.Active.Import".equals(measurand)) {
                                realtimeData.put("power", numericValue);
                            } else if ("Voltage".equals(measurand)) {
                                realtimeData.put("voltage", numericValue);
                            } else if ("Current.Import".equals(measurand)) {
                                realtimeData.put("current", numericValue);
                            }

                            sendMeterValueToFrontend(realtimeData);
                        }
                    }
                }
            }

            return response; // Return empty object

        } catch (Exception e) {
            System.err.println("Error in MeterValues: " + e.getMessage());
            return objectMapper.createObjectNode(); // Return empty on error too
        }
    }

    /**
     * 5. StopTransaction Handler - Matches REST API format with expiry validation
     */
    private ObjectNode handleStopTransaction(String deviceId, String messageId, JsonNode payload) {
        ObjectNode response = objectMapper.createObjectNode();
        ObjectNode idTagInfo = objectMapper.createObjectNode();

        try {
            Integer transactionId = payload.path("transactionId").asInt();
            Long meterStop = payload.path("meterStop").asLong();
            String timestampStr = payload.path("timestamp").asText();
            String idTag = payload.has("idTag") ? payload.path("idTag").asText() : null;

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

            // Validate IdTag if provided - with expiry check
            if (idTag != null && !idTag.isEmpty()) {
                var tagOpt = idTagInfoRepository.findByIdTagAndIdDevice(idTag, deviceId);
                LocalDateTime now = LocalDateTime.now();
                
                if (tagOpt.isEmpty()) {
                    idTagInfo.put("status", "Invalid");
                    System.out.println("❌ StopTransaction: IdTag not found: " + idTag);
                    response.set("idTagInfo", idTagInfo);
                    return response;
                }

                var tag = tagOpt.get();
                
                // Check expiry - same logic as REST API
                if (tag.getExpiryDate().isBefore(now)) {
                    idTagInfo.put("status", "Expired");
                    System.out.println("❌ StopTransaction: IdTag expired: " + idTag + 
                                     " (expired: " + tag.getExpiryDate() + ")");
                    response.set("idTagInfo", idTagInfo);
                    return response;
                }
                
                // Check status
                if (!"Accepted".equals(tag.getStatus())) {
                    idTagInfo.put("status", tag.getStatus());
                    System.out.println("❌ StopTransaction: IdTag status invalid: " + tag.getStatus());
                    response.set("idTagInfo", idTagInfo);
                    return response;
                }
            }

            // Process transactionData if present (for meter values)
            if (payload.has("transactionData")) {
                JsonNode transactionData = payload.path("transactionData");
                // Process meter values similar to MeterValues handler
                // This would be implemented similarly to MeterValues
            }

            // Use server time
            LocalDateTime endTime = LocalDateTime.now();
            System.out.println("⏰ [DEBUG] Using server end time: " + endTime);

            // Calculate duration
            long durationMinutes = 0;
            if (session.getStartTime() != null) {
                durationMinutes = java.time.Duration.between(session.getStartTime(), endTime).toMinutes();
            }

            // Calculate cost
            double cost = 0.0;
            if (meterStop != null) {
                double consumptionKWh = meterStop.doubleValue();
                cost = consumptionKWh * 0.15;

                session.setTotalConsumption(consumptionKWh);
                session.setAmount(cost);
                session.setEndTime(endTime);
                chargingSessionRepository.save(session);

                System.out.println("💰 Transaction completed:");
                System.out.println("   - Consumption: " + consumptionKWh + " kWh");
                System.out.println("   - Duration: " + durationMinutes + " minutes");
                System.out.println("   - Cost: $" + String.format("%.2f", cost));
            }

            // Return only idTagInfo with status (matches REST API)
            idTagInfo.put("status", "Accepted");
            response.set("idTagInfo", idTagInfo);

            // Send transaction details to frontend
            Map<String, Object> transactionDetails = new HashMap<>();
            transactionDetails.put("type", "TRANSACTION_COMPLETED");
            transactionDetails.put("transactionId", transactionId);
            transactionDetails.put("idDevice", deviceId);
            transactionDetails.put("idTag", idTag);
            transactionDetails.put("powerConsumed", session.getTotalConsumption());
            transactionDetails.put("durationMinutes", durationMinutes);
            transactionDetails.put("cost", String.format("%.2f", cost));
            transactionDetails.put("startTime", session.getStartTime().toString());
            transactionDetails.put("endTime", endTime.toString());
            transactionDetails.put("timestamp", LocalDateTime.now().toString());

            messagingTemplate.convertAndSend("/topic/device/" + deviceId, transactionDetails);
            messagingTemplate.convertAndSend("/topic/charging", transactionDetails);

        } catch (Exception e) {
            idTagInfo.put("status", "InternalError");
            response.set("idTagInfo", idTagInfo);
            System.err.println("❌ Error in handleStopTransaction: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    /**
     * Heartbeat Handler – matches REST API format
     */
    private ObjectNode handleHeartbeat() {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("currentTime", LocalDateTime.now().toString() + "Z");
        return response;
    }

    /**
     * StatusNotification Handler – matches REST API format (empty response)
     */
    private ObjectNode handleStatusNotification(String deviceId, JsonNode payload) {
        ObjectNode response = objectMapper.createObjectNode(); // Empty response
        try {
            String status = payload.path("status").asText();
            String errorCode = payload.path("errorCode").asText();
            
            // Update smart_plug status
            smartPlugRepository.findById(deviceId).ifPresent(plug -> {
                plug.setStatus(status);
                smartPlugRepository.save(plug);
            });
            
            // Return empty object as in REST API
        } catch (Exception e) {
            System.err.println("Error in StatusNotification: " + e.getMessage());
        }
        return response; // Empty object
    }

    private String handleCallResult(String deviceId, String messageId, JsonNode payload) {
        // Handle responses to our calls
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
        response.add(objectMapper.createObjectNode());
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

    /**
     * Generate a unique IdTag for a device - matches REST API logic
     */
    private String generateIdTag(String baseValue) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((baseValue + System.currentTimeMillis()).getBytes());
            String hex = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            return "IDT-" + hex.substring(0, 8).toUpperCase();
        } catch (Exception e) {
            return "IDT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    /**
     * Send meter values to frontend via STOMP
     */
    private void sendMeterValueToFrontend(Map<String, Object> meterData) {
        try {
            String deviceId = (String) meterData.get("idDevice");
            if (deviceId == null) {
                System.err.println("❌ Cannot send meter data: deviceId is null");
                return;
            }

            Map<String, Object> frontendMessage = new HashMap<>();
            frontendMessage.put("type", "METER_VALUES");
            frontendMessage.put("idDevice", deviceId);
            frontendMessage.put("timestamp", meterData.get("timestamp"));

            List<Map<String, Object>> sampledValues = new ArrayList<>();

            if (meterData.containsKey("power")) {
                Map<String, Object> powerSample = new HashMap<>();
                powerSample.put("value", meterData.get("power").toString());
                powerSample.put("measurand", "Power.Active.Import");
                powerSample.put("unit", "kW");
                sampledValues.add(powerSample);
            }

            if (meterData.containsKey("voltage")) {
                Map<String, Object> voltageSample = new HashMap<>();
                voltageSample.put("value", meterData.get("voltage").toString());
                voltageSample.put("measurand", "Voltage");
                voltageSample.put("unit", "V");
                sampledValues.add(voltageSample);
            }

            if (meterData.containsKey("current")) {
                Map<String, Object> currentSample = new HashMap<>();
                currentSample.put("value", meterData.get("current").toString());
                currentSample.put("measurand", "Current.Import");
                currentSample.put("unit", "A");
                sampledValues.add(currentSample);
            }

            if (meterData.containsKey("energy")) {
                Map<String, Object> energySample = new HashMap<>();
                energySample.put("value", meterData.get("energy").toString());
                energySample.put("measurand", "Energy.Active.Import.Register");
                energySample.put("unit", "kWh");
                sampledValues.add(energySample);
            }

            Map<String, Object> meterValueObj = new HashMap<>();
            meterValueObj.put("sampledValue", sampledValues);
            frontendMessage.put("meterValue", meterValueObj);

            messagingTemplate.convertAndSend("/topic/device/" + deviceId, frontendMessage);
            messagingTemplate.convertAndSend("/topic/charging", frontendMessage);

        } catch (Exception e) {
            System.err.println("❌ Error sending meter data to frontend: " + e.getMessage());
        }
    }

    /**
     * Helper method to check if IdTag is valid and not expired
     */
    private boolean isValidIdTag(String idTag, String deviceId) {
        Optional<IdTagInfo> tagOpt = idTagInfoRepository.findByIdTagAndIdDevice(idTag, deviceId);
        if (tagOpt.isEmpty()) {
            return false;
        }
        
        IdTagInfo tag = tagOpt.get();
        LocalDateTime now = LocalDateTime.now();
        
        // Check expiry and status
        return tag.getExpiryDate().isAfter(now) && "Accepted".equals(tag.getStatus());
    }

    /**
     * Get or create valid IdTag for device - matches REST API logic
     */
    private IdTagInfo getOrCreateValidIdTag(String deviceId) {
        LocalDateTime now = LocalDateTime.now();
        
        // Check for existing valid tag
        List<IdTagInfo> existingTags = idTagInfoRepository.findByIdDevice(deviceId);
        for (IdTagInfo tag : existingTags) {
            if (tag.getExpiryDate().isAfter(now) && "Accepted".equals(tag.getStatus())) {
                return tag;
            }
        }
        
        // Create new tag with 6 hours expiry
        SmartPlug plug = smartPlugRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("IdDevice not found: " + deviceId));
        
        String accountReference = (plug.getCebSerialNo() != null)
                ? plug.getCebSerialNo()
                : plug.getIdDevice();
        
        String idTag = generateIdTag(accountReference);
        LocalDateTime expiryDate = now.plusHours(6);
        
        IdTagInfo newTag = new IdTagInfo();
        newTag.setIdDevice(deviceId);
        newTag.setIdTag(idTag);
        newTag.setStatus("Accepted");
        newTag.setExpiryDate(expiryDate);
        newTag.setCreatedAt(now);
        
        return idTagInfoRepository.save(newTag);
    }
}
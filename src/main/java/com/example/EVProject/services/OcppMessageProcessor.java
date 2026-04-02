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
import java.util.concurrent.ConcurrentHashMap;

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

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    // Cache for last meter reading per transaction (power integration)
    private final Map<Integer, MeterReading> lastMeterReading = new ConcurrentHashMap<>();

    private static class MeterReading {
        LocalDateTime timestamp;
        double power; // in watts
        MeterReading(LocalDateTime ts, double p) { timestamp = ts; power = p; }
    }

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
                        idTagInfo.put("status", "Accepted");
                        idTagInfo.put("expiryDate", tag.getExpiryDate().atZone(ZoneOffset.UTC).toString());
                        idTagInfo.put("IdTag", tag.getIdTag());
                        System.out.println("✅ Authorize: returning idTag " + tag.getIdTag() + " for device " + deviceId);
                    } else {
                        idTagInfo.put("status", "Invalid");
                        System.out.println("⚠️ Authorize: latest IdTagInfo expired for device " + deviceId);
                    }
                } else {
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
     * 4. MeterValues Handler – integrates power over time to compute energy
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
                    LocalDateTime timestamp = LocalDateTime.parse(timestampStr.replace("Z", ""));
                    JsonNode sampledValues = meterValue.path("sampledValue");

                    double power = 0.0;
                    double voltage = 0.0;
                    double current = 0.0;

                    if (sampledValues.isArray()) {
                        for (JsonNode sample : sampledValues) {
                            String measurand = sample.path("measurand").asText();
                            double value = sample.path("value").asDouble();
                            switch (measurand) {
                                case "Power.Active.Import":
                                    power = value; // watts
                                    break;
                                case "Voltage":
                                    voltage = value;
                                    break;
                                case "Current.Import":
                                    current = value;
                                    break;
                                default:
                                    // ignore other measurands
                            }
                        }
                    }

                    // Integrate energy
                    MeterReading last = lastMeterReading.get(transactionId);
                    if (last != null) {
                        double durationHours = java.time.Duration.between(last.timestamp, timestamp).toMillis() / 3600000.0;
                        double energyKwh = last.power * durationHours / 1000.0; // power in W -> kW
                        double newTotal = session.getTotalConsumption() + energyKwh;
                        session.setTotalConsumption(newTotal);
                        chargingSessionRepository.save(session);
                    }
                    // Update last reading
                    lastMeterReading.put(transactionId, new MeterReading(timestamp, power));

                    // Send real-time data to frontend (simplified format)
                    Map<String, Object> realtimeData = new HashMap<>();
                    realtimeData.put("type", "METER_VALUES");
                    realtimeData.put("idDevice", deviceId);
                    realtimeData.put("transactionId", transactionId);
                    realtimeData.put("timestamp", timestampStr);
                    realtimeData.put("power", power);          // in watts
                    realtimeData.put("voltage", voltage);
                    realtimeData.put("current", current);
                    realtimeData.put("totalConsumption", session.getTotalConsumption()); // in kWh

                    messagingTemplate.convertAndSend("/topic/device/" + deviceId, realtimeData);
                    messagingTemplate.convertAndSend("/topic/charging", realtimeData);
                }
            }

            return objectMapper.createObjectNode(); // empty response

        } catch (Exception e) {
            ObjectNode errorResponse = objectMapper.createObjectNode();
            errorResponse.put("status", "Rejected");
            System.err.println("❌ Error in handleMeterValues: " + e.getMessage());
            e.printStackTrace();
            return errorResponse;
        }
    }

    /**
     * 5. StopTransaction Handler – finalizes session with integrated consumption
     */
    private ObjectNode handleStopTransaction(String deviceId, JsonNode payload) {
        ObjectNode response = objectMapper.createObjectNode();
        ObjectNode idTagInfo = objectMapper.createObjectNode();

        try {
            Integer transactionId = payload.path("transactionId").asInt();
            // meterStop is ignored – we use the integrated totalConsumption

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

            // Finalize session
            LocalDateTime endTime = LocalDateTime.now();
            session.setEndTime(endTime);

            double consumptionKWh = session.getTotalConsumption();
            double cost = consumptionKWh * 87.0; // Rs. 87 per kWh
            session.setAmount(cost);
            chargingSessionRepository.save(session);

            // Remove from cache
            lastMeterReading.remove(transactionId);

            // Calculate duration
            long durationMinutes = 0;
            if (session.getStartTime() != null) {
                durationMinutes = java.time.Duration.between(session.getStartTime(), endTime).toMinutes();
            }

            System.out.println("💰 Transaction completed:");
            System.out.println("   - Consumption: " + consumptionKWh + " kWh");
            System.out.println("   - Duration: " + durationMinutes + " minutes");
            System.out.println("   - Cost: Rs. " + String.format("%.2f", cost));

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

            idTagInfo.put("status", "Accepted");
            response.set("idTagInfo", idTagInfo);

        } catch (Exception e) {
            idTagInfo.put("status", "InternalError");
            response.set("idTagInfo", idTagInfo);
            System.err.println("❌ Error in handleStopTransaction: " + e.getMessage());
            e.printStackTrace();
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
        // Not used in this version
        return null;
    }

    private String handleCallError(String deviceId, String messageId, JsonNode payload) {
        // Not used in this version
        return null;
    }

    private String createCallResult(String messageId, JsonNode payload) {
        ArrayNode response = objectMapper.createArrayNode();
        response.add(3);
        response.add(messageId);
        response.add(payload);
        return response.toString();
    }

    private String createError(String errorCode, String errorDescription, String messageId) {
        ArrayNode response = objectMapper.createArrayNode();
        response.add(4);
        response.add(messageId != null ? messageId : "");
        response.add(errorCode);
        response.add(errorDescription);
        response.add(objectMapper.createObjectNode());
        return response.toString();
    }

    private ObjectNode createErrorPayload(String status, String message) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("status", status);
        if (message != null) payload.put("message", message);
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
            // silent fail
        }
    }
}
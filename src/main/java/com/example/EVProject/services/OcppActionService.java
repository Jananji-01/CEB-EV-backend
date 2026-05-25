package com.example.EVProject.services;

import com.example.EVProject.model.*;
import com.example.EVProject.repositories.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OcppActionService {

    @Autowired
    private ChargingSessionRepository chargingSessionRepository;
    @Autowired
    private IdTagInfoRepository idTagInfoRepository;
    @Autowired
    private SmartPlugRepository smartPlugRepository;
    @Autowired
    private EvOwnerRepository evOwnerRepository;
    @Autowired
    private BillingService billingService;

    @Autowired
    @Lazy
    private SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OcppActionService.class);

    private static class LastPowerReading {
        double powerW;
        LocalDateTime timestamp;
        LastPowerReading(double powerW, LocalDateTime timestamp) {
            this.powerW = powerW;
            this.timestamp = timestamp;
        }
    }
    private final Map<Integer, LastPowerReading> lastPowerReadings = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, LocalDateTime> deviceLastActivity = new ConcurrentHashMap<>();
    private final Set<String> devicesConsideredConnected = ConcurrentHashMap.newKeySet();

    public LocalDateTime getLastActivity(String deviceId) {
        return deviceLastActivity.get(deviceId);
    }

    private void updateLastActivity(String deviceId) {
        if (deviceId != null) {
            deviceLastActivity.put(deviceId, LocalDateTime.now());
            if (devicesConsideredConnected.add(deviceId)) {
                sendDeviceConnectionStatus(deviceId, true);
            }
        }
    }

    private void sendDeviceConnectionStatus(String deviceId, boolean connected) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "DEVICE_CONNECTION");
        msg.put("idDevice", deviceId);
        msg.put("connected", connected);
        msg.put("timestamp", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend("/topic/device/" + deviceId, msg);
        messagingTemplate.convertAndSend("/topic/charging", msg);
        System.out.println("Sent DEVICE_CONNECTION for " + deviceId + " connected=" + connected);
    }

    // ========== BootNotification ==========
    @Transactional
    public ObjectNode handleBootNotification(String deviceId, JsonNode payload) {
        updateLastActivity(deviceId);
        ObjectNode response = objectMapper.createObjectNode();
        try {
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
            response.put("status", "Accepted");
            response.put("currentTime", LocalDateTime.now(ZoneOffset.UTC).toString());
            response.put("interval", 300);
        } catch (Exception e) {
            response.put("status", "Rejected");
        }
        return response;
    }

    // ========== Authorize ==========
    @Transactional
    public ObjectNode handleAuthorize(String deviceId, JsonNode payload) {
        updateLastActivity(deviceId);
        ObjectNode response = objectMapper.createObjectNode();
        ObjectNode idTagInfo = objectMapper.createObjectNode();
        String deviceIdFromPayload = payload.path("IdDevice").asText();
        if (deviceIdFromPayload == null || deviceIdFromPayload.isEmpty() || !deviceId.equals(deviceIdFromPayload)) {
            idTagInfo.put("status", "Invalid");
        } else {
            LocalDateTime now = LocalDateTime.now();
            var tagOpt = idTagInfoRepository.findTopByIdDeviceOrderByCreatedAtDesc(deviceIdFromPayload);
            if (tagOpt.isPresent() && tagOpt.get().getExpiryDate().isAfter(now)) {
                IdTagInfo tag = tagOpt.get();
                idTagInfo.put("status", "Accepted");
                idTagInfo.put("expiryDate", tag.getExpiryDate().atZone(ZoneOffset.UTC).toString());
                idTagInfo.put("IdTag", tag.getIdTag());
            } else {
                idTagInfo.put("status", "Invalid");
            }
        }
        response.set("idTagInfo", idTagInfo);
        return response;
    }

    // ========== StartTransaction ==========
    @Transactional
    public ObjectNode handleStartTransaction(String deviceId, String messageId, JsonNode payload) {
        updateLastActivity(deviceId);
        ObjectNode response = objectMapper.createObjectNode();
        ObjectNode idTagInfo = objectMapper.createObjectNode();
        try {
            String idTag = payload.path("idTag").asText();
            int connectorId = payload.path("connectorId").asInt(1);
            long meterStart = payload.path("meterStart").asLong(0);
            var tagOpt = idTagInfoRepository.findByIdTagAndIdDevice(idTag, deviceId);
            if (tagOpt.isEmpty() || !"Accepted".equals(tagOpt.get().getStatus())) {
                idTagInfo.put("status", "Invalid");
                response.set("idTagInfo", idTagInfo);
                return response;
            }
            if (chargingSessionRepository.findByIdDeviceAndEndTimeIsNull(deviceId).isPresent()) {
                idTagInfo.put("status", "ConcurrentTx");
                response.set("idTagInfo", idTagInfo);
                return response;
            }
            String evOwnerAccountNo = null;
            Optional<EvOwner> ownerOpt = evOwnerRepository.findByIdTag(idTag);
            if (ownerOpt.isPresent()) {
                evOwnerAccountNo = ownerOpt.get().getEAccountNumber();
            }
            ChargingSession session = new ChargingSession();
            session.setIdDevice(deviceId);
            session.setStartTime(LocalDateTime.now());
            session.setChargingMode("FAST");
            session.setTotalConsumption(0.0);
            session.setAmount(0.0);
            session.setSoc(0.0);
            session.setEAccountNo(evOwnerAccountNo);
            ChargingSession saved = chargingSessionRepository.save(session);
            idTagInfo.put("status", "Accepted");
            response.set("idTagInfo", idTagInfo);
            response.put("transactionId", saved.getSessionId());
            Map<String, Object> frontendMessage = new HashMap<>();
            frontendMessage.put("type", "TRANSACTION_STARTED");
            frontendMessage.put("transactionId", saved.getSessionId());
            frontendMessage.put("idDevice", deviceId);
            frontendMessage.put("timestamp", LocalDateTime.now().toString());
            messagingTemplate.convertAndSend("/topic/device/" + deviceId, frontendMessage);
        } catch (Exception e) {
            idTagInfo.put("status", "InternalError");
            response.set("idTagInfo", idTagInfo);
            e.printStackTrace();
        }
        return response;
    }

    // ========== StopTransaction ==========
    @Transactional
    public ObjectNode handleStopTransaction(String deviceId, String messageId, JsonNode payload) {
        updateLastActivity(deviceId);
        ObjectNode response = objectMapper.createObjectNode();
        ObjectNode idTagInfo = objectMapper.createObjectNode();
        try {
            Integer transactionId = payload.path("transactionId").asInt();
            var sessionOpt = chargingSessionRepository.findById(transactionId);
            if (sessionOpt.isEmpty() || !deviceId.equals(sessionOpt.get().getIdDevice())) {
                idTagInfo.put("status", "Invalid");
                response.set("idTagInfo", idTagInfo);
                return response;
            }
            ChargingSession session = sessionOpt.get();
            LocalDateTime endTime = LocalDateTime.now();
            session.setEndTime(endTime);
            double totalKWh = session.getTotalConsumption() != null ? session.getTotalConsumption() : 0.0;
            long durationMinutes = java.time.Duration.between(session.getStartTime(), endTime).toMinutes();
            double cost = totalKWh * 87.0;
            session.setAmount(cost);
            chargingSessionRepository.save(session);
            final Integer finalTransactionId = transactionId;
            CompletableFuture.runAsync(() -> {
                try {
                    billingService.sendChargingDataToBilling(finalTransactionId);
                } catch (Exception e) {
                    logger.error("Billing API call failed for transaction {}: {}", finalTransactionId, e.getMessage());
                }
            });
            Map<String, Object> transactionDetails = new HashMap<>();
            transactionDetails.put("type", "TRANSACTION_COMPLETED");
            transactionDetails.put("transactionId", transactionId);
            transactionDetails.put("idDevice", deviceId);
            transactionDetails.put("powerConsumed", totalKWh);
            transactionDetails.put("cost", String.format("%.2f", cost));
            transactionDetails.put("startTime", session.getStartTime().toString());
            transactionDetails.put("endTime", endTime.toString());
            transactionDetails.put("durationMinutes", durationMinutes); 
            messagingTemplate.convertAndSend("/topic/device/" + deviceId, transactionDetails);
            messagingTemplate.convertAndSend("/topic/charging", transactionDetails);
            lastPowerReadings.remove(transactionId);
            idTagInfo.put("status", "Accepted");
            response.set("idTagInfo", idTagInfo);
        } catch (Exception e) {
            idTagInfo.put("status", "InternalError");
            response.set("idTagInfo", idTagInfo);
            e.printStackTrace();
        }
        return response;
    }

    // ========== MeterValues ==========
    @Transactional
    public ObjectNode handleMeterValues(String deviceId, JsonNode payload) {
        System.out.println("=== OcppActionService.handleMeterValues called for device " + deviceId);
        System.out.println("Payload: " + payload.toString());

        updateLastActivity(deviceId);
        ObjectNode response = objectMapper.createObjectNode();

        // Always send a response (even if something fails later)
        response.put("status", "Accepted");

        try {
            Integer transactionId = payload.path("transactionId").asInt();
            if (transactionId == null) {
                System.out.println("TransactionId is null, returning");
                return response;
            }

            var sessionOpt = chargingSessionRepository.findById(transactionId);
            if (sessionOpt.isEmpty()) {
                System.out.println("No session found for transactionId " + transactionId);
                // Still send a STOMP message with default values?
                // For now, just return.
                return response;
            }
            ChargingSession session = sessionOpt.get();

            JsonNode meterValueArray = payload.path("meterValue");
            System.out.println("meterValueArray size: " + meterValueArray.size());

            // Variables to hold latest readings
            Double powerW = null;
            Double voltage = null;
            Double current = null;
            Double energyWh = null;

            for (JsonNode meterValue : meterValueArray) {
                String timestampStr = meterValue.path("timestamp").asText();
                LocalDateTime timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_DATE_TIME);
                JsonNode sampledValues = meterValue.path("sampledValue");

                for (JsonNode sample : sampledValues) {
                    String measurand = sample.path("measurand").asText();
                    String value = sample.path("value").asText();
                    String unit = sample.path("unit").asText();
                    double numValue = Double.parseDouble(value);

                    System.out.println("Measurand: '" + measurand + "', value: " + numValue + ", unit: " + unit);

                    // Standard OCPP
                    switch (measurand) {
                        case "Energy.Active.Import.Register":
                            if ("Wh".equals(unit)) energyWh = numValue;
                            else if ("kWh".equals(unit)) energyWh = numValue * 1000;
                            break;
                        case "Power.Active.Import":
                            powerW = numValue;
                            break;
                        case "Voltage":
                            voltage = numValue;
                            break;
                        case "Current.Import":
                            current = numValue;
                            break;
                    }
                    // Non‑standard (your smart plug)
                    switch (measurand) {
                        case "Active_Power":
                            powerW = numValue;
                            break;
                        case "RMS_Voltage":
                            voltage = numValue;
                            break;
                        case "RMS_Current":
                            if ("mA".equals(unit)) {
                                current = numValue / 1000.0;
                            } else if ("A".equals(unit)) {
                                current = numValue;
                            } else {
                                current = numValue;
                            }
                            break;
                        case "Temperature_C":
                        case "Frequency_Hz":
                            // ignore
                            break;
                    }
                }

                System.out.println("Extracted - powerW: " + powerW + ", voltage: " + voltage + ", current: " + current + ", energyWh: " + energyWh);

                // Update total energy
                if (energyWh != null) {
                    double energyKWh = energyWh / 1000.0;
                    session.setTotalConsumption(energyKWh);
                    chargingSessionRepository.save(session);
                    System.out.println("Updated total consumption via energyWh: " + energyKWh + " kWh");
                } else if (powerW != null) {
                    LastPowerReading last = lastPowerReadings.get(transactionId);
                    if (last != null) {
                        long deltaSeconds = java.time.Duration.between(last.timestamp, timestamp).getSeconds();
                        if (deltaSeconds > 0) {
                            double powerKW = powerW / 1000.0;
                            double deltaHours = deltaSeconds / 3600.0;
                            double added = powerKW * deltaHours;
                            session.setTotalConsumption(session.getTotalConsumption() + added);
                            chargingSessionRepository.save(session);
                            System.out.println("Integrated power: added " + added + " kWh, new total: " + session.getTotalConsumption());
                        }
                    }
                    lastPowerReadings.put(transactionId, new LastPowerReading(powerW, timestamp));
                }

                // Build STOMP message
                Map<String, Object> realtime = new HashMap<>();
                realtime.put("type", "METER_VALUES");
                realtime.put("idDevice", deviceId);
                realtime.put("transactionId", transactionId);
                realtime.put("timestamp", timestampStr);
                if (powerW != null) realtime.put("power", powerW);
                if (voltage != null) realtime.put("voltage", voltage);
                if (current != null) realtime.put("current", current);
                // Always include total consumption (even if zero)
                double totalKWh = session.getTotalConsumption() != null ? session.getTotalConsumption() : 0.0;
                realtime.put("totalConsumption", totalKWh);

                System.out.println("Sending METER_VALUES to /topic/device/" + deviceId + " with data: " + realtime);
                try {
                    messagingTemplate.convertAndSend("/topic/device/" + deviceId, realtime);
                    System.out.println("✅ STOMP message sent successfully");
                } catch (Exception e) {
                    System.err.println("❌ Failed to send STOMP message: " + e.getMessage());
                    e.printStackTrace();
                }
            } // end for each meterValue

        } catch (Exception e) {
            System.err.println("Error in handleMeterValues: " + e.getMessage());
            e.printStackTrace();
            // Still return accepted response to the device
        }
        return response;
    }

    // ========== Heartbeat ==========
    public ObjectNode handleHeartbeat() {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("currentTime", LocalDateTime.now(ZoneOffset.UTC).toString());
        return response;
    }

    // ========== StatusNotification ==========
    @Transactional
    public ObjectNode handleStatusNotification(String deviceId, JsonNode payload) {
        updateLastActivity(deviceId);
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
}
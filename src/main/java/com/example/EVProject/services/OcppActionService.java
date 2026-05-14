package com.example.EVProject.services;

import com.example.EVProject.model.*;
import com.example.EVProject.repositories.*;
import com.example.EVProject.services.BillingService;
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

    // For power integration fallback (when device sends Power.Active.Import instead of Energy register)
    private static class LastPowerReading {
        double powerW;
        LocalDateTime timestamp;
        LastPowerReading(double powerW, LocalDateTime timestamp) {
            this.powerW = powerW;
            this.timestamp = timestamp;
        }
    }
    private final Map<Integer, LastPowerReading> lastPowerReadings = new ConcurrentHashMap<>();

    // ========== BootNotification ==========
    @Transactional
    public ObjectNode handleBootNotification(String deviceId, JsonNode payload) {
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
        ObjectNode response = objectMapper.createObjectNode();
        ObjectNode idTagInfo = objectMapper.createObjectNode();

        try {
            String idTag = payload.path("idTag").asText();
            int connectorId = payload.path("connectorId").asInt(1);
            long meterStart = payload.path("meterStart").asLong(0);

            // Validate idTag
            var tagOpt = idTagInfoRepository.findByIdTagAndIdDevice(idTag, deviceId);
            if (tagOpt.isEmpty() || !"Accepted".equals(tagOpt.get(0).getStatus())) {
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

            // Retrieve EV owner account number
            String evOwnerAccountNo = null;
            Optional<EvOwner> ownerOpt = evOwnerRepository.findByIdTag(idTag);
            if (ownerOpt.isPresent()) {
                evOwnerAccountNo = ownerOpt.get().getEAccountNumber();
            }

            // Create new charging session
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

            // Notify frontend via STOMP
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
        ObjectNode response = objectMapper.createObjectNode();
        ObjectNode idTagInfo = objectMapper.createObjectNode();

        try {
            Integer transactionId = payload.path("transactionId").asInt();
            // meterStop is ignored – we use server‑accumulated energy
            String timestampStr = payload.path("timestamp").asText();

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
            double cost = totalKWh * 87.0; // Rs. 87 per kWh
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

            // Notify frontend
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

            // Clear power integration cache
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
        ObjectNode response = objectMapper.createObjectNode();

        try {
            Integer transactionId = payload.path("transactionId").asInt();
            if (transactionId == null) return objectMapper.createObjectNode();

            var sessionOpt = chargingSessionRepository.findById(transactionId);
            if (sessionOpt.isEmpty()) return objectMapper.createObjectNode();
            ChargingSession session = sessionOpt.get();

            JsonNode meterValueArray = payload.path("meterValue");
            for (JsonNode meterValue : meterValueArray) {
                String timestampStr = meterValue.path("timestamp").asText();
                LocalDateTime timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_DATE_TIME);
                JsonNode sampledValues = meterValue.path("sampledValue");

                Double energyWh = null;
                Double powerW = null;
                Double voltage = null;
                Double current = null;

                for (JsonNode sample : sampledValues) {
                    String measurand = sample.path("measurand").asText();
                    String value = sample.path("value").asText();
                    String unit = sample.path("unit").asText();
                    double numValue = Double.parseDouble(value);

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
                }

                // Update total energy
                if (energyWh != null) {
                    double energyKWh = energyWh / 1000.0;
                    session.setTotalConsumption(energyKWh);
                    chargingSessionRepository.save(session);
                } else if (powerW != null) {
                    // Fallback: integrate power over time
                    LastPowerReading last = lastPowerReadings.get(transactionId);
                    if (last != null) {
                        long deltaSeconds = java.time.Duration.between(last.timestamp, timestamp).getSeconds();
                        if (deltaSeconds > 0) {
                            double powerKW = powerW / 1000.0;
                            double deltaHours = deltaSeconds / 3600.0;
                            double added = powerKW * deltaHours;
                            session.setTotalConsumption(session.getTotalConsumption() + added);
                            chargingSessionRepository.save(session);
                        }
                    }
                    lastPowerReadings.put(transactionId, new LastPowerReading(powerW, timestamp));
                }

                // Send real‑time data to frontend
                Map<String, Object> realtime = new HashMap<>();
                realtime.put("type", "METER_VALUES");
                realtime.put("idDevice", deviceId);
                realtime.put("transactionId", transactionId);
                realtime.put("timestamp", timestampStr);
                if (powerW != null) realtime.put("power", powerW);
                if (voltage != null) realtime.put("voltage", voltage);
                if (current != null) realtime.put("current", current);
                realtime.put("totalConsumption", session.getTotalConsumption());
                messagingTemplate.convertAndSend("/topic/device/" + deviceId, realtime);
            }
            response.put("status", "Accepted");
        } catch (Exception e) {
            response.put("status", "Rejected");
            e.printStackTrace();
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
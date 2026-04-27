package com.example.EVProject.services;  // Note: services not service

import com.example.EVProject.dto.*;
import com.example.EVProject.model.ChargingSession;
import com.example.EVProject.model.OcppMessageLog;
import com.example.EVProject.repositories.ChargingSessionRepository;
import com.example.EVProject.repositories.OcppMessageLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final ChargingSessionRepository chargingSessionRepository;
    private final OcppMessageLogRepository ocppMessageLogRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Transactional(readOnly = true)
    public List<ChargingSession> filterSessions(SessionFilterRequest filter) {
        if (filter.getDeviceId() != null && !filter.getDeviceId().isEmpty()
                && filter.getEAccountNo() != null && !filter.getEAccountNo().isEmpty()) {
            return chargingSessionRepository.findByIdDeviceAndEAccountNo(
                    filter.getDeviceId(), filter.getEAccountNo());
        } else if (filter.getDeviceId() != null && !filter.getDeviceId().isEmpty()) {
            return chargingSessionRepository.findByIdDevice(filter.getDeviceId());
        } else if (filter.getEAccountNo() != null && !filter.getEAccountNo().isEmpty()) {
            return chargingSessionRepository.findByEAccountNo(filter.getEAccountNo());
        } else {
            return chargingSessionRepository.findAll();
        }
    }

    @Transactional(readOnly = true)
    public SessionDetailDTO getSessionWithEnergyCalculation(Integer sessionId) {
        ChargingSession session = chargingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        SessionDetailDTO detail = new SessionDetailDTO();
        detail.setSessionId(session.getSessionId());
        detail.setIdDevice(session.getIdDevice());
        detail.setEAccountNo(session.getEAccountNo());
        detail.setStartTime(session.getStartTime());
        detail.setEndTime(session.getEndTime());
        detail.setTotalConsumption(session.getTotalConsumption());
        detail.setAmount(session.getAmount());
        detail.setStatus(session.getStatus());
        detail.setChargingMode(session.getChargingMode());

        // Get power readings from OCPP messages
        List<PowerReadingDTO> readings = extractPowerReadings(session);
        detail.setPowerReadings(readings);

        // Calculate energy
        EnergyCalculationDTO energyCalc = calculateEnergy(readings);
        detail.setEnergyCalculation(energyCalc);

        return detail;
    }

    private List<PowerReadingDTO> extractPowerReadings(ChargingSession session) {
        List<PowerReadingDTO> readings = new ArrayList<>();

        if (session.getStartTime() == null) {
            return readings;
        }

        LocalDateTime endTime = session.getEndTime() != null ? session.getEndTime() : LocalDateTime.now();

        // Find OCPP messages with MeterValues action during the session
        List<OcppMessageLog> meterValues = ocppMessageLogRepository
                .findByIdDeviceAndActionAndReceivedAtBetween(
                        session.getIdDevice(), "MeterValues", session.getStartTime(), endTime);

        double cumulativeEnergy = 0.0;
        PowerReadingDTO previousReading = null;

        for (OcppMessageLog message : meterValues) {
            try {
                JsonNode payload = objectMapper.readTree(message.getPayload());

                // Try to extract power from payload
                Double activePower = extractActivePower(payload);

                if (activePower != null && message.getReceivedAt() != null) {
                    PowerReadingDTO reading = new PowerReadingDTO();
                    reading.setId(message.getId());
                    reading.setTimestamp(message.getReceivedAt());
                    reading.setActivePower(activePower);

                    // Calculate energy for interval
                    if (previousReading != null) {
                        double avgPower = (Math.abs(previousReading.getActivePower()) + Math.abs(activePower)) / 2;
                        double timeDeltaSeconds = Duration.between(
                                previousReading.getTimestamp(), message.getReceivedAt()).getSeconds();
                        double timeDeltaHours = timeDeltaSeconds / 3600.0;
                        double energyWh = avgPower * timeDeltaHours;
                        cumulativeEnergy += energyWh;

                        reading.setEnergyWh(energyWh);
                        reading.setCumulativeEnergyKwh(cumulativeEnergy / 1000.0);
                    } else {
                        reading.setEnergyWh(0.0);
                        reading.setCumulativeEnergyKwh(0.0);
                    }

                    readings.add(reading);
                    previousReading = reading;
                }
            } catch (Exception e) {
                log.error("Error parsing MeterValues payload: {}", e.getMessage());
            }
        }

        return readings;
    }

    private Double extractActivePower(JsonNode payload) {
        // Try different paths to find active power
        if (payload.has("meterValue")) {
            JsonNode meterValue = payload.get("meterValue");
            if (meterValue.isArray() && meterValue.size() > 0) {
                JsonNode sampledValue = meterValue.get(0).get("sampledValue");
                if (sampledValue != null && sampledValue.isArray()) {
                    for (JsonNode sv : sampledValue) {
                        String measurand = sv.has("measurand") ? sv.get("measurand").asText() : "";
                        if ("Power.Active.Import".equals(measurand) || "Power.Active".equals(measurand)) {
                            return sv.get("value").asDouble();
                        }
                    }
                }
            }
        }

        // Try direct power field
        if (payload.has("power")) {
            return payload.get("power").asDouble();
        }

        return null;
    }

    private EnergyCalculationDTO calculateEnergy(List<PowerReadingDTO> readings) {
        EnergyCalculationDTO calc = new EnergyCalculationDTO();

        if (readings.isEmpty()) {
            calc.setTotalEnergyKwh(0.0);
            calc.setTotalReadings(0);
            calc.setCalculationMethod("No power readings available");
            calc.setCalculationTime(LocalDateTime.now());
            calc.setSampleIntervals(new ArrayList<>());
            return calc;
        }

        double totalEnergyKwh = readings.stream()
                .mapToDouble(PowerReadingDTO::getCumulativeEnergyKwh)
                .max()
                .orElse(0.0);

        calc.setTotalEnergyKwh(totalEnergyKwh);
        calc.setTotalReadings(readings.size());
        calc.setCalculationMethod("Trapezoidal integration method using Active_Power readings");
        calc.setCalculationTime(LocalDateTime.now());

        // Create sample intervals (first 10 readings for demonstration)
        List<IntervalCalculationDTO> sampleIntervals = new ArrayList<>();
        int sampleSize = Math.min(10, readings.size());

        for (int i = 1; i < sampleSize; i++) {
            PowerReadingDTO prev = readings.get(i - 1);
            PowerReadingDTO curr = readings.get(i);

            IntervalCalculationDTO interval = new IntervalCalculationDTO();
            interval.setReadingId(curr.getId() != null ? curr.getId().intValue() : i);
            interval.setStartTime(prev.getTimestamp());
            interval.setEndTime(curr.getTimestamp());

            double avgPower = (Math.abs(prev.getActivePower()) + Math.abs(curr.getActivePower())) / 2;
            interval.setAvgPower(avgPower);

            double timeDeltaSeconds = Duration.between(prev.getTimestamp(), curr.getTimestamp()).getSeconds();
            interval.setTimeDeltaHours(timeDeltaSeconds / 3600.0);
            interval.setEnergyWh(curr.getEnergyWh());
            interval.setCumulativeKwh(curr.getCumulativeEnergyKwh());

            sampleIntervals.add(interval);
        }

        calc.setSampleIntervals(sampleIntervals);
        return calc;
    }
}
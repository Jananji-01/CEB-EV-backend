package com.example.EVProject.services;

import com.example.EVProject.dto.MeterValueResponse;
import com.example.EVProject.model.*;
import com.example.EVProject.dto.MeterValueRequest;
import com.example.EVProject.repositories.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeterValueService {

    private final MeterValueRepository meterRepo;
    private final SampledValueRepository sampleRepo;
    private final ChargingSessionRepository sessionRepo;

    @Transactional
    public void saveMeterValues(MeterValueRequest request) {
        ChargingSession session = sessionRepo.findById(Math.toIntExact(request.getTransactionId().longValue()))
                .orElseThrow(() -> new RuntimeException("Session not found: " + request.getTransactionId()));

        if (request.getMeterValue() == null || request.getMeterValue().isEmpty()) {
            throw new IllegalArgumentException("MeterValue list cannot be null or empty");
        }

        for (MeterValueRequest.MeterReading reading : request.getMeterValue()) {
            if (reading.getSampledValue() == null || reading.getSampledValue().isEmpty()) {
                throw new IllegalArgumentException(
                        "sampledValue list cannot be null or empty for timestamp: " + reading.getTimestamp());
            }

            OffsetDateTime timestamp;
            try {
                timestamp = OffsetDateTime.parse(reading.getTimestamp());
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid ISO 8601 timestamp: " + reading.getTimestamp());
            }

            MeterValue meterValue = MeterValue.builder()
                    .connectorId(request.getConnectorId())
                    .timestamp(timestamp.toLocalDateTime())
                    .session(session)
                    .build();

            meterValue = meterRepo.save(meterValue);

            MeterValue finalMeterValue = meterValue;
            List<SampledValue> samples = reading.getSampledValue().stream().map(s -> SampledValue.builder()
                    .value(Double.parseDouble(s.getValue()))
                    .context(s.getContext())
                    .format(s.getFormat())
                    .measurand(s.getMeasurand())
                    .location(s.getLocation())
                    .unit(s.getUnit())
                    .meterValue(finalMeterValue)
                    .build()).collect(Collectors.toList());

            sampleRepo.saveAll(samples);
        }
    }

//    @Transactional
//    public List<MeterValueResponse> getMeterValuesBySession(Long sessionId) {
//        List<MeterValue> meterValues = meterRepo.findAllBySession_SessionId(sessionId);
//
//        return meterValues.stream().map(mv -> {
//            MeterValueResponse dto = new MeterValueResponse();
//            dto.setMeterId(mv.getMeterId());
//            dto.setConnectorId(mv.getConnectorId());
//            dto.setTimestamp(mv.getTimestamp());
//
//            if (mv.getSession() != null && mv.getSession().getSmartPlug() != null) {
//                dto.setIdDevice(mv.getSession().getSmartPlug().getIdDevice());
//                if (mv.getSession().getSmartPlug().getStation() != null) {
//                    dto.setStationId(mv.getSession().getSmartPlug().getStation().getStationId());
//                }
//            }
//
//            List<MeterValueResponse.SampleData> samples = mv.getSampledValues().stream().map(s -> {
//                MeterValueResponse.SampleData sd = new MeterValueResponse.SampleData();
//                sd.setValue(s.getValue());
//                sd.setContext(s.getContext());
//                sd.setFormat(s.getFormat());
//                sd.setMeasurand(s.getMeasurand());
//                sd.setLocation(s.getLocation());
//                sd.setUnit(s.getUnit());
//                return sd;
//            }).collect(Collectors.toList());
//
//            dto.setSampledValues(samples);
//            return dto;
//        }).collect(Collectors.toList());
//    }
}

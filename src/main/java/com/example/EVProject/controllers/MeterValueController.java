package com.example.EVProject.controllers;

import com.example.EVProject.dto.MeterValueRequest;
import com.example.EVProject.dto.MeterValueResponse;
import com.example.EVProject.model.OcppMessageLog;
import com.example.EVProject.repositories.OcppMessageLogRepository;
import com.example.EVProject.services.MeterValueService;
import com.example.EVProject.utils.IdDeviceValidator;
import com.example.EVProject.utils.OcppMessageParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meter-values")
@RequiredArgsConstructor
public class MeterValueController {

    private final MeterValueService meterService;
    private final IdDeviceValidator idDeviceValidator;
    private final OcppMessageLogRepository messageLogRepo;

    @PostMapping
    public ResponseEntity<?> handleMeterValues(
            @RequestBody String rawBody,
            @RequestHeader("IdDevice") String idDevice) {

        LocalDateTime receivedTime = LocalDateTime.now();

        try {
            // 1. Validate IdDevice
            idDeviceValidator.validate(idDevice);

            // 2. Parse OCPP array [MessageTypeId, MessageId, Action, Payload]
            var parsed = OcppMessageParser.parse(rawBody);

            if (parsed.messageTypeId() != 2 || !"MeterValues".equalsIgnoreCase(parsed.action())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid message type or action. Expected MeterValues"));
            }

            var payload = parsed.payload();

            // 3. Extract connectorId & transactionId
            Integer connectorId = payload.has("connectorId") ? payload.get("connectorId").asInt() : 1;
            Integer transactionId = payload.has("transactionId") ? payload.get("transactionId").asInt() : null;

            if (transactionId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Missing transactionId in payload"));
            }

            // 4. Map meterValue array to DTO
            ObjectMapper mapper = new ObjectMapper();
            MeterValueRequest request = new MeterValueRequest();
            request.setConnectorId(connectorId);
            request.setTransactionId(transactionId);

            if (payload.has("meterValue") && payload.get("meterValue").isArray()) {
                request.setMeterValue(mapper.convertValue(
                        payload.get("meterValue"),
                        mapper.getTypeFactory().constructCollectionType(
                                java.util.List.class, MeterValueRequest.MeterReading.class
                        )
                ));
            }

            // 5. Save meter values
            meterService.saveMeterValues(request);

            // 6. Build MeterValues.conf response: [3, MessageId, {}]
            Object[] ocppResponse = new Object[]{
                    3,
                    parsed.messageId(),
                    new HashMap<>()
            };

            // 7. Log incoming & outgoing messages
            if (messageLogRepo != null) {
                OcppMessageLog log = new OcppMessageLog();
                log.setIdDevice(idDevice);
                log.setMessageId(parsed.messageId());
                log.setAction(parsed.action());
                log.setMessageTypeId(parsed.messageTypeId());
                log.setPayload(payload.toString());
                log.setResponse(mapper.writeValueAsString(ocppResponse));
                log.setReceivedAt(receivedTime);
                log.setRespondedAt(LocalDateTime.now());
                messageLogRepo.save(log);
            }

            // 8. Return OCPP .conf
            return ResponseEntity.ok(ocppResponse);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_SERVER_ERROR", "message", e.getMessage()));
        }
    }


//    @GetMapping("/{sessionId}")
//    public ResponseEntity<List<MeterValueResponse>> getMeterValuesBySession(@PathVariable Long sessionId) {
//        List<MeterValueResponse> response = meterService.getMeterValuesBySession(sessionId);
//        return ResponseEntity.ok(response);
//    }


}

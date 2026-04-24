package com.example.EVProject.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.example.EVProject.services.OcppActionService;

import com.example.EVProject.model.OcppMessageLog;
import com.example.EVProject.repositories.OcppMessageLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OcppMessageProcessor {

    @Autowired
    private OcppActionService ocppActionService;

    @Autowired
    private OcppMessageLogRepository messageLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String processMessage(String deviceId, String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            if (!root.isArray() || root.size() < 3) {
                return createError("ProtocolError", "Invalid message format", null);
            }

            int messageTypeId = root.get(0).asInt();
            String messageId = root.get(1).asText();
            String action;
            JsonNode payload;

            switch (messageTypeId) {
                case 2: // CALL
                    action = root.get(2).asText();
                    payload = root.size() > 3 ? root.get(3) : objectMapper.createObjectNode();
                    break;
                case 3: // CALLRESULT
                    action = "CALLRESULT";
                    payload = root.size() > 2 ? root.get(2) : objectMapper.createObjectNode();
                    break;
                case 4: // CALLERROR
                    action = "CALLERROR";
                    payload = root.size() > 2 ? root.get(2) : objectMapper.createObjectNode();
                    break;
                default:
                    return createError("FormatViolation", "Unknown message type", messageId);
            }

            logMessage(deviceId, messageId, action, messageTypeId, payload.toString(), "INCOMING");

            switch (messageTypeId) {
                case 2:
                    return handleCall(deviceId, messageId, action, payload);
                case 3:
                    return handleCallResult(deviceId, messageId, payload);
                case 4:
                    return handleCallError(deviceId, messageId, payload);
                default:
                    return createError("FormatViolation", "Unknown message type", messageId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return createError("InternalError", e.getMessage(), null);
        }
    }

    private String handleCall(String deviceId, String messageId, String action, JsonNode payload) {
        ObjectNode responsePayload;

        switch (action) {
            case "BootNotification":
                responsePayload = ocppActionService.handleBootNotification(deviceId, payload);
                break;
            case "Authorize":
                responsePayload = ocppActionService.handleAuthorize(deviceId, payload);
                break;
            case "StartTransaction":
                responsePayload = ocppActionService.handleStartTransaction(deviceId, messageId, payload);
                break;
            case "StopTransaction":
                responsePayload = ocppActionService.handleStopTransaction(deviceId, messageId, payload);
                break;
            case "MeterValues":
                responsePayload = ocppActionService.handleMeterValues(deviceId, payload);
                break;
            case "Heartbeat":
                responsePayload = ocppActionService.handleHeartbeat();
                break;
            case "StatusNotification":
                responsePayload = ocppActionService.handleStatusNotification(deviceId, payload);
                break;
            default:
                responsePayload = createErrorPayload("NotSupported", "Action not supported");
        }

        String response = createCallResult(messageId, responsePayload);
        logMessage(deviceId, messageId, action, 3, responsePayload.toString(), "OUTGOING");
        return response;
    }

    private String handleCallResult(String deviceId, String messageId, JsonNode payload) {
        // Handle responses to our calls (e.g., RemoteStartTransaction.conf) – optional
        return null;
    }

    private String handleCallError(String deviceId, String messageId, JsonNode payload) {
        // Handle error responses – optional
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
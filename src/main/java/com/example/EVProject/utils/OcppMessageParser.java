package com.example.EVProject.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OcppMessageParser {

    public static ParsedOcppMessage parse(String rawJson) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(rawJson);

        int messageTypeId = root.get(0).asInt();
        String messageId = root.get(1).asText();
        String action = root.get(2).asText();
        JsonNode payload = root.get(3);

        return new ParsedOcppMessage(messageTypeId, messageId, action, payload);
    }

    public record ParsedOcppMessage(int messageTypeId, String messageId, String action, JsonNode payload) {}
}

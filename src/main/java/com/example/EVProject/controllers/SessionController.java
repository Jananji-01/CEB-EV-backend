package com.example.EVProject.controllers;

import com.example.EVProject.dto.SessionDetailDTO;
import com.example.EVProject.dto.SessionFilterRequest;
import com.example.EVProject.model.ChargingSession;
import com.example.EVProject.services.SessionService;
import com.example.EVProject.services.SessionPdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SessionController {

    private final SessionService sessionService;
    private final SessionPdfService sessionPdfService;

    // Test endpoint to check if controller is working
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "Session Controller is working!");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    // Simple filter using GET parameters (easier to test)
    @GetMapping("/filter")
    public ResponseEntity<List<ChargingSession>> filterSessionsGet(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String eAccountNo,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {

        log.info("GET filter called with deviceId: {}, eAccountNo: {}", deviceId, eAccountNo);

        SessionFilterRequest filter = new SessionFilterRequest();
        filter.setDeviceId(deviceId);
        filter.setEAccountNo(eAccountNo);
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);

        List<ChargingSession> sessions = sessionService.filterSessions(filter);
        return ResponseEntity.ok(sessions);
    }

    // POST filter with JSON body
    @PostMapping("/filter")
    public ResponseEntity<?> filterSessions(@RequestBody SessionFilterRequest filter) {
        try {
            log.info("POST filter called with filter: {}", filter);

            if (filter == null) {
                filter = new SessionFilterRequest();
            }

            List<ChargingSession> sessions = sessionService.filterSessions(filter);
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            log.error("Error in filterSessions: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Get all sessions (no filter)
    @GetMapping("/all")
    public ResponseEntity<List<ChargingSession>> getAllSessions() {
        log.info("Getting all sessions");
        SessionFilterRequest filter = new SessionFilterRequest();
        List<ChargingSession> sessions = sessionService.filterSessions(filter);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/{sessionId}/detail")
    public ResponseEntity<?> getSessionDetail(@PathVariable Integer sessionId) {
        try {
            log.info("Getting session detail for ID: {}", sessionId);
            SessionDetailDTO detail = sessionService.getSessionWithEnergyCalculation(sessionId);
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            log.error("Error getting session detail: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Session not found with ID: " + sessionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @GetMapping("/{sessionId}/report")
    public ResponseEntity<String> getSessionReportHtml(@PathVariable Integer sessionId) {
        try {
            log.info("Generating HTML report for session ID: {}", sessionId);
            SessionDetailDTO session = sessionService.getSessionWithEnergyCalculation(sessionId);
            String html = sessionPdfService.generateHtmlReport(session);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                    .body(html);
        } catch (Exception e) {
            log.error("Error generating report: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("<html><body><h1>Error</h1><p>" + e.getMessage() + "</p></body></html>");
        }
    }

    @GetMapping("/{sessionId}/download")
    public ResponseEntity<byte[]> downloadSessionReport(@PathVariable Integer sessionId) {
        try {
            log.info("Downloading PDF report for session ID: {}", sessionId);
            SessionDetailDTO session = sessionService.getSessionWithEnergyCalculation(sessionId);
            String html = sessionPdfService.generateHtmlReport(session);

            // Return HTML with instruction to print/save as PDF
            String instructions = "<!DOCTYPE html><html><head>" +
                    "<meta charset='UTF-8'>" +
                    "<title>Session Report - Session " + sessionId + "</title>" +
                    "</head><body>" +
                    "<script>window.onload = function() { window.print(); }</script>" +
                    html +
                    "</body></html>";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=session_" + sessionId + "_report.html")
                    .body(instructions.getBytes());
        } catch (Exception e) {
            log.error("Error downloading report: {}", e.getMessage());
            String errorHtml = "<html><body><h1>Error</h1><p>" + e.getMessage() + "</p></body></html>";
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                    .body(errorHtml.getBytes());
        }
    }
}


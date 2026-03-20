package com.example.EVProject.controllers;

import com.example.EVProject.dto.SmartPlugDTO;
import com.example.EVProject.model.ChargingSession;
import com.example.EVProject.model.ChargingStation;
import com.example.EVProject.repositories.ChargingStationRepository;
import com.example.EVProject.repositories.SmartPlugRepository;
import com.example.EVProject.services.ChargingSessionService;
import com.example.EVProject.services.OcppWebSocketService;
import com.example.EVProject.services.SmartPlugService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/smartplugs")
public class AdminSmartPlugController {

    @Autowired
    private SmartPlugService smartPlugService;
    
    @Autowired
    private SmartPlugRepository smartPlugRepository;
    
    @Autowired
    private ChargingSessionService chargingSessionService;
    
    @Autowired
    private ChargingStationRepository chargingStationRepository;
    
    @Autowired
    private OcppWebSocketService ocppWebSocketService;

    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllSmartPlugsWithStatus() {
        try {
            List<SmartPlugDTO> plugs = smartPlugService.getAllSmartPlugs();
            
            List<Map<String, Object>> result = plugs.stream().map(plug -> {
                Map<String, Object> details = new HashMap<>();
                details.put("smartPlug", plug);
                
                // Get WebSocket connection status
                boolean isConnected = ocppWebSocketService.isDeviceConnected(plug.getIdDevice());
                details.put("connected", isConnected);
                
                // Get active charging session
                ChargingSession activeSession = chargingSessionService.getActiveSession(plug.getIdDevice());
                details.put("isCharging", activeSession != null);
                
                if (activeSession != null) {
                    Map<String, Object> session = new HashMap<>();
                    session.put("sessionId", activeSession.getSessionId());
                    session.put("startTime", activeSession.getStartTime());
                    session.put("totalConsumption", activeSession.getTotalConsumption());
                    session.put("chargingMode", activeSession.getChargingMode());
                    details.put("activeSession", session);
                }
                
                // Get charging station info if available
                if (plug.getStationId() != null) {
                    chargingStationRepository.findById(plug.getStationId()).ifPresent(station -> {
                        details.put("stationName", getStationName(station));
                        details.put("stationLocation", String.format("%.4f, %.4f", 
                            station.getLatitude(), station.getLongitude()));
                    });
                }
                
                return details;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/station/{stationId}")
    public ResponseEntity<List<Map<String, Object>>> getSmartPlugsByStation(@PathVariable Integer stationId) {
        try {
            List<SmartPlugDTO> allPlugs = smartPlugService.getAllSmartPlugs();
            
            List<Map<String, Object>> stationPlugs = allPlugs.stream()
                .filter(plug -> plug.getStationId() != null && plug.getStationId().equals(stationId))
                .map(plug -> {
                    Map<String, Object> details = new HashMap<>();
                    details.put("smartPlug", plug);
                    
                    boolean isConnected = ocppWebSocketService.isDeviceConnected(plug.getIdDevice());
                    details.put("connected", isConnected);
                    
                    boolean isCharging = chargingSessionService.getActiveSession(plug.getIdDevice()) != null;
                    details.put("isCharging", isCharging);
                    
                    return details;
                }).collect(Collectors.toList());
            
            return ResponseEntity.ok(stationPlugs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{deviceId}/details")
    public ResponseEntity<Map<String, Object>> getSmartPlugDetails(@PathVariable String deviceId) {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // Get basic device info
            SmartPlugDTO plug = smartPlugService.getSmartPlugById(deviceId);
            if (plug == null) {
                return ResponseEntity.notFound().build();
            }
            
            response.put("deviceInfo", plug);
            
            // Connection status
            boolean isConnected = ocppWebSocketService.isDeviceConnected(deviceId);
            response.put("connected", isConnected);
            
            // Active session details
            ChargingSession activeSession = chargingSessionService.getActiveSession(deviceId);
            response.put("isCharging", activeSession != null);
            
            if (activeSession != null) {
                Map<String, Object> sessionDetails = new HashMap<>();
                sessionDetails.put("sessionId", activeSession.getSessionId());
                sessionDetails.put("startTime", activeSession.getStartTime());
                sessionDetails.put("duration", calculateDuration(activeSession.getStartTime()));
                sessionDetails.put("totalConsumption", activeSession.getTotalConsumption());
                sessionDetails.put("chargingMode", activeSession.getChargingMode());
                response.put("activeSession", sessionDetails);
            }
            
            // Station information
            if (plug.getStationId() != null) {
                chargingStationRepository.findById(plug.getStationId()).ifPresent(station -> {
                    Map<String, Object> stationInfo = new HashMap<>();
                    stationInfo.put("stationId", station.getStationId());
                    stationInfo.put("name", getStationName(station));
                    stationInfo.put("location", String.format("%.4f, %.4f", 
                        station.getLatitude(), station.getLongitude()));
                    stationInfo.put("solarPowerAvailable", station.getSolarPowerAvailable());
                    stationInfo.put("status", station.getStatus());
                    response.put("stationInfo", stationInfo);
                });
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/dashboard-summary")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        try {
            List<SmartPlugDTO> allPlugs = smartPlugService.getAllSmartPlugs();
            
            Map<String, Object> summary = new HashMap<>();
            
            int totalPlugs = allPlugs.size();
            int connectedPlugs = 0;
            int chargingPlugs = 0;
            
            // Count stations
            Set<Integer> stations = new HashSet<>();
            
            for (SmartPlugDTO plug : allPlugs) {
                boolean isConnected = ocppWebSocketService.isDeviceConnected(plug.getIdDevice());
                boolean isCharging = chargingSessionService.getActiveSession(plug.getIdDevice()) != null;
                
                if (isConnected) connectedPlugs++;
                if (isCharging) chargingPlugs++;
                if (plug.getStationId() != null) {
                    stations.add(plug.getStationId());
                }
            }
            
            summary.put("totalSmartPlugs", totalPlugs);
            summary.put("connectedPlugs", connectedPlugs);
            summary.put("chargingPlugs", chargingPlugs);
            summary.put("availablePlugs", connectedPlugs - chargingPlugs);
            summary.put("disconnectedPlugs", totalPlugs - connectedPlugs);
            summary.put("stationsWithPlugs", stations.size());
            summary.put("lastUpdated", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/stations")
    public ResponseEntity<List<Map<String, Object>>> getStationsWithSmartPlugs() {
        try {
            List<ChargingStation> allStations = chargingStationRepository.findAll();
            List<SmartPlugDTO> allPlugs = smartPlugService.getAllSmartPlugs();
            
            List<Map<String, Object>> result = allStations.stream()
                .map(station -> {
                    Map<String, Object> stationData = new HashMap<>();
                    stationData.put("stationId", station.getStationId());
                    stationData.put("name", getStationName(station));
                    stationData.put("location", String.format("%.4f, %.4f", 
                        station.getLatitude(), station.getLongitude()));
                    stationData.put("status", station.getStatus());
                    
                    // Find smart plugs in this station
                    List<SmartPlugDTO> stationPlugs = allPlugs.stream()
                        .filter(plug -> plug.getStationId() != null && 
                                plug.getStationId().equals(station.getStationId()))
                        .collect(Collectors.toList());
                    
                    stationData.put("smartPlugCount", stationPlugs.size());
                    stationData.put("smartPlugs", stationPlugs.stream()
                        .map(plug -> {
                            Map<String, Object> plugInfo = new HashMap<>();
                            plugInfo.put("idDevice", plug.getIdDevice());
                            plugInfo.put("cebSerialNo", plug.getCebSerialNo());
                            plugInfo.put("connected", ocppWebSocketService.isDeviceConnected(plug.getIdDevice()));
                            plugInfo.put("isCharging", 
                                chargingSessionService.getActiveSession(plug.getIdDevice()) != null);
                            return plugInfo;
                        }).collect(Collectors.toList()));
                    
                    return stationData;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/connected")
    public ResponseEntity<List<Map<String, Object>>> getConnectedSmartPlugs() {
        try {
            List<SmartPlugDTO> allPlugs = smartPlugService.getAllSmartPlugs();
            
            List<Map<String, Object>> connectedPlugs = allPlugs.stream()
                .filter(plug -> ocppWebSocketService.isDeviceConnected(plug.getIdDevice()))
                .map(plug -> {
                    Map<String, Object> details = new HashMap<>();
                    details.put("smartPlug", plug);
                    details.put("connected", true);
                    
                    ChargingSession activeSession = chargingSessionService.getActiveSession(plug.getIdDevice());
                    details.put("isCharging", activeSession != null);
                    
                    if (activeSession != null) {
                        details.put("sessionStartTime", activeSession.getStartTime());
                        details.put("sessionDuration", calculateDuration(activeSession.getStartTime()));
                    }
                    
                    return details;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(connectedPlugs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/charging")
    public ResponseEntity<List<Map<String, Object>>> getChargingSmartPlugs() {
        try {
            List<SmartPlugDTO> allPlugs = smartPlugService.getAllSmartPlugs();
            
            List<Map<String, Object>> chargingPlugs = allPlugs.stream()
                .filter(plug -> {
                    ChargingSession session = chargingSessionService.getActiveSession(plug.getIdDevice());
                    return session != null;
                })
                .map(plug -> {
                    Map<String, Object> details = new HashMap<>();
                    details.put("smartPlug", plug);
                    details.put("connected", true);
                    
                    ChargingSession activeSession = chargingSessionService.getActiveSession(plug.getIdDevice());
                    if (activeSession != null) {
                        details.put("sessionId", activeSession.getSessionId());
                        details.put("startTime", activeSession.getStartTime());
                        details.put("duration", calculateDuration(activeSession.getStartTime()));
                        details.put("totalConsumption", activeSession.getTotalConsumption());
                        details.put("chargingMode", activeSession.getChargingMode());
                    }
                    
                    return details;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(chargingPlugs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getStationName(ChargingStation station) {
        if (station == null) return "Unknown Station";
        
        // Simple naming based on station ID
        Map<Integer, String> stationNames = new HashMap<>();
        stationNames.put(1, "Main Station");
        stationNames.put(2, "Solar Station");
        stationNames.put(4, "West Station");
        stationNames.put(5, "North Station");
        stationNames.put(6, "South Station");
        stationNames.put(7, "East Station");
        stationNames.put(8, "Central Station");
        stationNames.put(9, "Outlet Station");
        
        return stationNames.getOrDefault(station.getStationId(), 
            "Station " + station.getStationId());
    }

    private String calculateDuration(java.time.LocalDateTime startTime) {
        if (startTime == null) return "00:00:00";
        
        java.time.Duration duration = java.time.Duration.between(startTime, java.time.LocalDateTime.now());
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
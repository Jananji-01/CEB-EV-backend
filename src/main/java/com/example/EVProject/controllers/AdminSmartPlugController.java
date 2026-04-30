package com.example.EVProject.controllers;

import com.example.EVProject.dto.SmartPlugDTO;
import com.example.EVProject.model.ChargingSession;
import com.example.EVProject.model.ChargingStation;
import com.example.EVProject.model.SmartPlug;
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
                    Optional<ChargingStation> stationOpt = chargingStationRepository.findById(plug.getStationId());
                    if (stationOpt.isPresent()) {
                        ChargingStation station = stationOpt.get();
                        String stationName = getStationDisplayName(station);
                        details.put("stationName", stationName);
                        details.put("stationId", station.getStationId());
                        details.put("stationLocation", String.format("%.4f, %.4f",
                                station.getLatitude(), station.getLongitude()));
                        details.put("latitude", station.getLatitude());
                        details.put("longitude", station.getLongitude());
                        details.put("assigned", true);
                    }
                } else {
                    details.put("stationName", "Unassigned");
                    details.put("stationId", null);
                    details.put("stationLocation", "Not Assigned");
                    details.put("latitude", null);
                    details.put("longitude", null);
                    details.put("assigned", false);
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
    public ResponseEntity<Map<String, Object>> getSmartPlugsByStation(@PathVariable Integer stationId) {
        try {
            List<SmartPlugDTO> allPlugs = smartPlugService.getAllSmartPlugs();
            Optional<ChargingStation> stationOpt = chargingStationRepository.findById(stationId);

            if (stationOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ChargingStation station = stationOpt.get();

            // Filter plugs for this station
            List<SmartPlugDTO> stationPlugs = allPlugs.stream()
                    .filter(plug -> plug.getStationId() != null && plug.getStationId().equals(stationId))
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("stationId", station.getStationId());
            response.put("stationName", getStationDisplayName(station));
            response.put("location", String.format("%.4f, %.4f",
                    station.getLatitude(), station.getLongitude()));
            response.put("latitude", station.getLatitude());
            response.put("longitude", station.getLongitude());
            response.put("status", station.getStatus());

            // Calculate totals
            int totalPlugs = stationPlugs.size();
            int connectedPlugs = 0;
            int chargingPlugs = 0;

            List<Map<String, Object>> plugsWithStatus = new ArrayList<>();

            for (SmartPlugDTO plug : stationPlugs) {
                Map<String, Object> plugDetails = new HashMap<>();
                plugDetails.put("smartPlug", plug);

                boolean isConnected = ocppWebSocketService.isDeviceConnected(plug.getIdDevice());
                boolean isCharging = chargingSessionService.getActiveSession(plug.getIdDevice()) != null;

                if (isConnected) connectedPlugs++;
                if (isCharging) chargingPlugs++;

                plugDetails.put("connected", isConnected);
                plugDetails.put("isCharging", isCharging);
                plugDetails.put("cebSerialNo", plug.getCebSerialNo());
                plugDetails.put("maximumOutput", plug.getMaximumOutput());

                plugsWithStatus.add(plugDetails);
            }

            response.put("totalSmartPlugs", totalPlugs);
            response.put("connectedSmartPlugs", connectedPlugs);
            response.put("chargingSmartPlugs", chargingPlugs);
            response.put("availableSmartPlugs", connectedPlugs - chargingPlugs);
            response.put("disconnectedSmartPlugs", totalPlugs - connectedPlugs);
            response.put("smartPlugs", plugsWithStatus);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }







    // Add this endpoint to assign multiple smart plugs to stations
    @PostMapping("/batch-assign")
    public ResponseEntity<Map<String, Object>> batchAssignSmartPlugs(@RequestBody Map<String, Object> assignments) {
        try {
            List<Map<String, String>> assignmentsList = (List<Map<String, String>>) assignments.get("assignments");
            int updatedCount = 0;

            for (Map<String, String> assignment : assignmentsList) {
                String deviceId = assignment.get("deviceId");
                Integer stationId = Integer.parseInt(assignment.get("stationId"));

                Optional<SmartPlug> plugOpt = smartPlugRepository.findById(deviceId);
                if (plugOpt.isPresent()) {
                    SmartPlug plug = plugOpt.get();
                    plug.setStationId(stationId);
                    smartPlugRepository.save(plug);
                    updatedCount++;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Assigned " + updatedCount + " smart plugs to stations");
            response.put("updatedCount", updatedCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }


    // Add this endpoint to your AdminSmartPlugController class
    @GetMapping("/stations/with-smartplugs")
    public ResponseEntity<List<Map<String, Object>>> getStationsWithSmartPlugsCount() {
        try {
            List<ChargingStation> allStations = chargingStationRepository.findAll();
            List<SmartPlugDTO> allPlugs = smartPlugService.getAllSmartPlugs();

            List<Map<String, Object>> result = new ArrayList<>();

            for (ChargingStation station : allStations) {
                // Get smart plugs for this station
                List<SmartPlugDTO> stationPlugs = allPlugs.stream()
                        .filter(plug -> plug.getStationId() != null &&
                                plug.getStationId().equals(station.getStationId()))
                        .collect(Collectors.toList());

                // Count total smart plugs
                int totalPlugs = stationPlugs.size();

                // Count connected smart plugs
                long connectedCount = stationPlugs.stream()
                        .filter(plug -> ocppWebSocketService.isDeviceConnected(plug.getIdDevice()))
                        .count();

                // Count charging smart plugs
                long chargingCount = stationPlugs.stream()
                        .filter(plug -> chargingSessionService.getActiveSession(plug.getIdDevice()) != null)
                        .count();

                Map<String, Object> stationData = new HashMap<>();
                stationData.put("stationId", station.getStationId());
                stationData.put("stationName", getStationDisplayName(station));
                stationData.put("location", String.format("%.4f, %.4f",
                        station.getLatitude(), station.getLongitude()));
                stationData.put("latitude", station.getLatitude());
                stationData.put("longitude", station.getLongitude());
                stationData.put("status", station.getStatus());

                // Show total smart plug count prominently
                stationData.put("smartPlugsCount", totalPlugs);
                stationData.put("totalSmartPlugs", totalPlugs);
                stationData.put("connectedSmartPlugs", connectedCount);
                stationData.put("chargingSmartPlugs", chargingCount);
                stationData.put("availableSmartPlugs", connectedCount - chargingCount);
                stationData.put("disconnectedSmartPlugs", totalPlugs - connectedCount);

                // Add detailed smart plugs list
                List<Map<String, Object>> plugsDetail = new ArrayList<>();
                for (SmartPlugDTO plug : stationPlugs) {
                    Map<String, Object> plugDetail = new HashMap<>();
                    plugDetail.put("idDevice", plug.getIdDevice());
                    plugDetail.put("cebSerialNo", plug.getCebSerialNo());
                    plugDetail.put("maximumOutput", plug.getMaximumOutput());

                    boolean isConnected = ocppWebSocketService.isDeviceConnected(plug.getIdDevice());
                    boolean isCharging = chargingSessionService.getActiveSession(plug.getIdDevice()) != null;

                    plugDetail.put("connected", isConnected);
                    plugDetail.put("isCharging", isCharging);
                    plugDetail.put("status", isConnected ? (isCharging ? "Charging" : "Connected") : "Offline");

                    if (isCharging) {
                        ChargingSession session = chargingSessionService.getActiveSession(plug.getIdDevice());
                        if (session != null) {
                            Map<String, Object> sessionInfo = new HashMap<>();
                            sessionInfo.put("sessionId", session.getSessionId());
                            sessionInfo.put("startTime", session.getStartTime());
                            sessionInfo.put("duration", calculateDuration(session.getStartTime()));
                            sessionInfo.put("totalConsumption", session.getTotalConsumption());
                            plugDetail.put("activeSession", sessionInfo);
                        }
                    }

                    plugsDetail.add(plugDetail);
                }

                stationData.put("smartPlugs", plugsDetail);

                // Only include stations that have smart plugs
                if (totalPlugs > 0) {
                    result.add(stationData);
                }
            }

            // Sort by station name
            result.sort((s1, s2) -> {
                String name1 = (String) s1.get("stationName");
                String name2 = (String) s2.get("stationName");
                return name1.compareTo(name2);
            });

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
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
                Optional<ChargingStation> stationOpt = chargingStationRepository.findById(plug.getStationId());
                if (stationOpt.isPresent()) {
                    ChargingStation station = stationOpt.get();
                    Map<String, Object> stationInfo = new HashMap<>();
                    stationInfo.put("stationId", station.getStationId());
                    String stationName = getStationDisplayName(station);
                    stationInfo.put("name", stationName);
                    stationInfo.put("location", String.format("%.4f, %.4f",
                            station.getLatitude(), station.getLongitude()));
                    stationInfo.put("solarPowerAvailable", station.getSolarPowerAvailable());
                    stationInfo.put("status", station.getStatus());
                    response.put("stationInfo", stationInfo);
                }
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

            // Count stations with plugs
            Set<Integer> stationsWithPlugs = new HashSet<>();

            for (SmartPlugDTO plug : allPlugs) {
                boolean isConnected = ocppWebSocketService.isDeviceConnected(plug.getIdDevice());
                boolean isCharging = chargingSessionService.getActiveSession(plug.getIdDevice()) != null;

                if (isConnected) connectedPlugs++;
                if (isCharging) chargingPlugs++;
                if (plug.getStationId() != null) {
                    stationsWithPlugs.add(plug.getStationId());
                }
            }

            summary.put("totalSmartPlugs", totalPlugs);
            summary.put("connectedPlugs", connectedPlugs);
            summary.put("chargingPlugs", chargingPlugs);
            summary.put("availablePlugs", connectedPlugs - chargingPlugs);
            summary.put("disconnectedPlugs", totalPlugs - connectedPlugs);
            summary.put("stationsWithPlugs", stationsWithPlugs.size());
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
                        String stationName = getStationDisplayName(station);
                        stationData.put("name", stationName);
                        stationData.put("location", String.format("%.4f, %.4f",
                                station.getLatitude(), station.getLongitude()));
                        stationData.put("latitude", station.getLatitude());
                        stationData.put("longitude", station.getLongitude());
                        stationData.put("status", station.getStatus());

                        // Find smart plugs in this station
                        List<SmartPlugDTO> stationPlugs = allPlugs.stream()
                                .filter(plug -> plug.getStationId() != null &&
                                        plug.getStationId().equals(station.getStationId()))
                                .collect(Collectors.toList());

                        // Calculate connected and charging counts
                        long totalCount = stationPlugs.size();
                        long connectedCount = stationPlugs.stream()
                                .filter(plug -> ocppWebSocketService.isDeviceConnected(plug.getIdDevice()))
                                .count();

                        long chargingCount = stationPlugs.stream()
                                .filter(plug -> chargingSessionService.getActiveSession(plug.getIdDevice()) != null)
                                .count();

                        stationData.put("totalSmartPlugs", totalCount);
                        stationData.put("connectedSmartPlugs", connectedCount);
                        stationData.put("chargingSmartPlugs", chargingCount);
                        stationData.put("availableSmartPlugs", connectedCount - chargingCount);
                        stationData.put("disconnectedSmartPlugs", totalCount - connectedCount);

                        stationData.put("smartPlugs", stationPlugs.stream()
                                .map(plug -> {
                                    Map<String, Object> plugInfo = new HashMap<>();
                                    plugInfo.put("idDevice", plug.getIdDevice());
                                    plugInfo.put("cebSerialNo", plug.getCebSerialNo());
                                    plugInfo.put("maximumOutput", plug.getMaximumOutput());
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

    @PutMapping("/{deviceId}/assign-station")
    public ResponseEntity<Map<String, Object>> assignSmartPlugToStation(
            @PathVariable String deviceId,
            @RequestParam Integer stationId) {
        try {
            // Find the smart plug entity directly from repository
            Optional<SmartPlug> plugOpt = smartPlugRepository.findById(deviceId);
            if (plugOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Smart plug not found with ID: " + deviceId);
                return ResponseEntity.notFound().build();
            }

            Optional<ChargingStation> stationOpt = chargingStationRepository.findById(stationId);
            if (stationOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Station not found with ID: " + stationId);
                return ResponseEntity.badRequest().body(error);
            }

            // Update the smart plug's station assignment
            SmartPlug plug = plugOpt.get();
            plug.setStationId(stationId);
            smartPlugRepository.save(plug);

            ChargingStation station = stationOpt.get();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Smart plug " + deviceId + " assigned to station " + stationId);
            response.put("deviceId", deviceId);
            response.put("stationId", stationId);
            response.put("stationName", getStationDisplayName(station));
            response.put("stationLocation", String.format("%.4f, %.4f",
                    station.getLatitude(), station.getLongitude()));
            response.put("latitude", station.getLatitude());
            response.put("longitude", station.getLongitude());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to assign smart plug: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
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

                        if (plug.getStationId() != null) {
                            chargingStationRepository.findById(plug.getStationId()).ifPresent(station -> {
                                details.put("stationId", station.getStationId());
                                details.put("stationName", getStationDisplayName(station));
                            });
                        } else {
                            details.put("stationName", "Unassigned");
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

                        if (plug.getStationId() != null) {
                            chargingStationRepository.findById(plug.getStationId()).ifPresent(station -> {
                                details.put("stationId", station.getStationId());
                                details.put("stationName", getStationDisplayName(station));
                            });
                        } else {
                            details.put("stationName", "Unassigned");
                        }

                        return details;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(chargingPlugs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getStationDisplayName(ChargingStation station) {
        if (station == null) return "Unknown Station";

        // Try to get name using common field names
        try {
            java.lang.reflect.Method[] methods = station.getClass().getMethods();
            for (java.lang.reflect.Method method : methods) {
                String methodName = method.getName();
                if ((methodName.equals("getName") || methodName.equals("getStationName") ||
                        methodName.equals("getStName")) && method.getParameterCount() == 0) {
                    Object result = method.invoke(station);
                    if (result != null && !result.toString().trim().isEmpty()) {
                        return result.toString();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore reflection errors, use fallback
        }

        return "Station " + station.getStationId();
    }

    private String calculateDuration(LocalDateTime startTime) {
        if (startTime == null) return "00:00:00";

        java.time.Duration duration = java.time.Duration.between(startTime, LocalDateTime.now());
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
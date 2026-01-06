package com.example.EVProject.services;

import com.example.EVProject.dto.ChargingStationDTO;
import com.example.EVProject.model.ChargingStation;
import com.example.EVProject.repositories.ChargingStationRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChargingStationService {

    @Autowired
    private ChargingStationRepository repository;

    public List<ChargingStationDTO> getAllStations() {
        return repository.findAll().stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public ChargingStationDTO getStationById(Integer id) {
        ChargingStation station = repository.findById(id).orElse(null);
        return station != null ? convertToDto(station) : null;
    }

    public ChargingStationDTO saveStation(ChargingStationDTO dto) {
        ChargingStation station = convertToEntity(dto);
        ChargingStation saved = repository.save(station);
        return convertToDto(saved);
    }

    public void deleteStation(Integer id) {
        repository.deleteById(id);
    }

    @Transactional
    public Map<String, Object> updateChargingStationStatus(
            Integer stationId, Integer status, String errorCode, LocalDateTime timestamp) {


        Map<Integer, String> statusMap = Map.of(
                1, "Available",
                2, "Preparing",
                3, "Charging",
                4, "Finishing",
                5, "Unavailable"
        );

        if (!statusMap.containsKey(status)) {
            throw new IllegalArgumentException("Invalid status code: " + status);
        }

        ChargingStation station = repository.findById(stationId)
                .orElseThrow(() -> new EntityNotFoundException("Charging station not found with ID: " + stationId));

        // ✅ Update fields according to OCPP message
        station.setStatus(statusMap.get(status));
        station.setErrorCode(errorCode);
        station.setTimestamp(timestamp);

        repository.save(station);

        // ✅ Return OCPP confirmation-like response
        return Map.of(
                "connectorId", stationId,
                "status", statusMap.get(status),
                "errorCode", errorCode,
                "timestamp", timestamp != null ? timestamp.toString() : LocalDateTime.now().toString()
        );
    }


    private ChargingStationDTO convertToDto(ChargingStation station) {
        ChargingStationDTO dto = new ChargingStationDTO();
        dto.setStationId(station.getStationId());
        dto.setLatitude(station.getLatitude());
        dto.setLongitude(station.getLongitude());
        dto.setSolarPowerAvailable(station.getSolarPowerAvailable());
        dto.setStatus(station.getStatus());
        dto.setSolarOwnerId(station.getSolarOwnerId());
        return dto;
    }

    private ChargingStation convertToEntity(ChargingStationDTO dto) {
        ChargingStation station = new ChargingStation();
        station.setStationId(dto.getStationId());
        station.setLatitude(dto.getLatitude());
        station.setLongitude(dto.getLongitude());
        station.setSolarPowerAvailable(dto.getSolarPowerAvailable());
        station.setStatus(dto.getStatus());
        station.setSolarOwnerId(dto.getSolarOwnerId());
        return station;
    }
}

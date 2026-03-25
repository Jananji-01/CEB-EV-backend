package com.example.EVProject.controllers;

import com.example.EVProject.dto.ChargingSessionDTO;
import com.example.EVProject.dto.SolarOwnerConsumptionDTO;
import com.example.EVProject.services.ChargingSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/charging-sessions")
public class ChargingSessionController {

    @Autowired
    private ChargingSessionService service;

    @GetMapping
    public List<ChargingSessionDTO> getAllSessions() {
        return service.getAllSessions();
    }

    @GetMapping("/{id}")
    public ChargingSessionDTO getSessionById(@PathVariable Integer id) {
        return service.getSessionById(id);
    }

    @GetMapping("/by-solar-owner/{ownerId}")
    public List<ChargingSessionDTO> getSessionsBySolarOwner(@PathVariable Integer ownerId) {
        return service.getSessionsBySolarOwnerId(ownerId);
    }

    @GetMapping("/total-consumption/by-solar-owner/{ownerId}")
    public Double getTotalConsumptionBySolarOwner(@PathVariable Integer ownerId) {
        return service.getTotalConsumptionBySolarOwner(ownerId);
    }

    @GetMapping("/total-consumption/by-owner/{ownerId}")
    public SolarOwnerConsumptionDTO getConsumptionByOwner(@PathVariable Integer ownerId) {
        return service.getMonthlyConsumptionByOwner(ownerId);
    }

    @GetMapping("/current-month")
    public List<ChargingSessionDTO> getSessionsForCurrentMonth() {
        return service.getSessionsForCurrentMonth();
    }



    @PostMapping
    public ChargingSessionDTO saveSession(@RequestBody ChargingSessionDTO dto) {
        return service.saveSession(dto);
    }

    @DeleteMapping("/{id}")
    public void deleteSession(@PathVariable Integer id) {
        service.deleteSession(id);
    }
}

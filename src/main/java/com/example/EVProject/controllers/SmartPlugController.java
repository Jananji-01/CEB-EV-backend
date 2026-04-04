package com.example.EVProject.controllers;

import com.example.EVProject.dto.AssignSmartPlugRequestDTO;
import com.example.EVProject.dto.SmartPlugDTO;
import com.example.EVProject.services.SmartPlugService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/smart-plugs")
public class SmartPlugController {

    @Autowired
    private SmartPlugService service;

    @GetMapping
    public List<SmartPlugDTO> getAllSmartPlugs() {
        return service.getAllSmartPlugs();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SmartPlugDTO> getSmartPlugById(@PathVariable String id) {
        SmartPlugDTO plug = service.getSmartPlugById(id);
        if (plug == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(plug);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerSmartPlug(@RequestBody SmartPlugDTO registrationDTO) {
        try {
            // Validate required fields
            if (registrationDTO.getIdDevice() == null || registrationDTO.getIdDevice().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Device ID is required"));
            }
            
            SmartPlugDTO registeredPlug = service.registerSmartPlug(registrationDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredPlug);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/{idDevice}/assign")
    public ResponseEntity<?> assignSmartPlugToStation(
            @PathVariable String idDevice,
            @RequestBody AssignSmartPlugRequestDTO request) {
        try {
            SmartPlugDTO assigned = service.assignToStation(idDevice, request);
            return ResponseEntity.ok(assigned);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSmartPlug(@PathVariable String id, @RequestBody SmartPlugDTO updateDTO) {
        try {
            SmartPlugDTO updatedPlug = service.updateSmartPlug(id, updateDTO);
            return ResponseEntity.ok(updatedPlug);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @PostMapping("/{id}/regenerate-qr")
    public ResponseEntity<?> regenerateQRCode(@PathVariable String id) {
        try {
            SmartPlugDTO updatedPlug = service.regenerateQRCode(id);
            return ResponseEntity.ok(updatedPlug);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSmartPlug(@PathVariable String id) {
        try {
            service.deleteSmartPlug(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
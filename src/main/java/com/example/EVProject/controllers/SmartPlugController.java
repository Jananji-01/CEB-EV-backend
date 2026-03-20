//package com.example.EVProject.controllers;
//
//import com.example.EVProject.dto.SmartPlugDTO;
//import com.example.EVProject.services.SmartPlugService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/smart-plugs")
//public class SmartPlugController {
//
//    @Autowired
//    private SmartPlugService service;
//
//    @GetMapping
//    public List<SmartPlugDTO> getAllSmartPlugs() {
//        return service.getAllSmartPlugs();
//    }
//
//    @GetMapping("/{id}")
//    public SmartPlugDTO getSmartPlugById(@PathVariable String id) {
//        return service.getSmartPlugById(id);
//    }
//
//    @PostMapping
//    public SmartPlugDTO saveSmartPlug(@RequestBody SmartPlugDTO dto) {
//        return service.saveSmartPlug(dto);
//    }
//
//    @DeleteMapping("/{id}")
//    public void deleteSmartPlug(@PathVariable String id) {
//        service.deleteSmartPlug(id);
//    }
//}


package com.example.EVProject.controllers;

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

    // NEW API → Get QR Code data
    @GetMapping("/{id}/qr")
    public String getQRCodeData(@PathVariable String id) {

        SmartPlugDTO plug = service.getSmartPlugById(id);

        if (plug == null) {
            return "Smart plug not found";
        }

        return plug.getQrCodeData();
    }

    // NEW API → Regenerate QR Code
    @PostMapping("/{id}/regenerate-qr")
    public SmartPlugDTO regenerateQRCode(@PathVariable String id) {

        SmartPlugDTO existing = service.getSmartPlugById(id);

        if (existing == null) {
            throw new RuntimeException("Smart plug not found");
        }

        // regenerate QR by saving again with same ID
        return service.saveSmartPlug(existing);
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
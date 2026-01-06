package com.example.EVProject.controllers;

import com.example.EVProject.dto.RooftopSolarOwnerDTO;
import com.example.EVProject.model.RooftopSolarOwner;
import com.example.EVProject.services.RooftopSolarOwnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/solar-owners")
//@CrossOrigin(origins = "*")
public class RooftopSolarOwnerController {

    @Autowired
    private RooftopSolarOwnerService rooftopSolarOwnerService;

    @GetMapping
    public ResponseEntity<List<RooftopSolarOwner>> getAllRooftopSolarOwners() {
        List<RooftopSolarOwner> solarOwners = rooftopSolarOwnerService.getAllRooftopSolarOwners();
        return ResponseEntity.ok(solarOwners);
    }

    @GetMapping("/{solarOwnerId}")
    public ResponseEntity<RooftopSolarOwner> getRooftopSolarOwnerById(@PathVariable Integer solarOwnerId) {
        Optional<RooftopSolarOwner> solarOwner = rooftopSolarOwnerService.getRooftopSolarOwnerById(solarOwnerId);
        return solarOwner.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<RooftopSolarOwner> getRooftopSolarOwnerByUsername(@PathVariable String username) {
        Optional<RooftopSolarOwner> solarOwner = rooftopSolarOwnerService.getRooftopSolarOwnerByUsername(username);
        return solarOwner.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<RooftopSolarOwner> createRooftopSolarOwner(@Valid @RequestBody RooftopSolarOwnerDTO rooftopSolarOwnerDTO) {
        try {
            RooftopSolarOwner solarOwner = rooftopSolarOwnerService.createRooftopSolarOwner(rooftopSolarOwnerDTO);
            return ResponseEntity.ok(solarOwner);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{solarOwnerId}")
    public ResponseEntity<RooftopSolarOwner> updateRooftopSolarOwner(@PathVariable Integer solarOwnerId,
                                                                     @Valid @RequestBody RooftopSolarOwnerDTO rooftopSolarOwnerDTO) {
        try {
            RooftopSolarOwner solarOwner = rooftopSolarOwnerService.updateRooftopSolarOwner(solarOwnerId, rooftopSolarOwnerDTO);
            return ResponseEntity.ok(solarOwner);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{solarOwnerId}")
    public ResponseEntity<Void> deleteRooftopSolarOwner(@PathVariable Integer solarOwnerId) {
        try {
            rooftopSolarOwnerService.deleteRooftopSolarOwner(solarOwnerId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
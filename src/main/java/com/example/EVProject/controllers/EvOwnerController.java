package com.example.EVProject.controllers;

import com.example.EVProject.dto.EvOwnerDTO;
import com.example.EVProject.model.EvOwner;
import com.example.EVProject.services.EvOwnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/ev-owners")
//@CrossOrigin(origins = "*")
public class EvOwnerController {

    @Autowired
    private EvOwnerService evOwnerService;

    @GetMapping
    public ResponseEntity<List<EvOwner>> getAllEvOwners() {
        List<EvOwner> evOwners = evOwnerService.getAllEvOwners();
        return ResponseEntity.ok(evOwners);
    }

    @GetMapping("/{evOwnerId}")
    public ResponseEntity<EvOwner> getEvOwnerById(@PathVariable Integer evOwnerId) {
        Optional<EvOwner> evOwner = evOwnerService.getEvOwnerById(evOwnerId);
        return evOwner.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EvOwner> createEvOwner(@Valid @RequestBody EvOwnerDTO evOwnerDTO) {
        try {
            EvOwner evOwner = evOwnerService.createEvOwner(evOwnerDTO);
            return ResponseEntity.ok(evOwner);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{evOwnerId}")
    public ResponseEntity<EvOwner> updateEvOwner(@PathVariable Integer evOwnerId,
                                                 @Valid @RequestBody EvOwnerDTO evOwnerDTO) {
        try {
            EvOwner evOwner = evOwnerService.updateEvOwner(evOwnerId, evOwnerDTO);
            return ResponseEntity.ok(evOwner);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{evOwnerId}")
    public ResponseEntity<Void> deleteEvOwner(@PathVariable Integer evOwnerId) {
        try {
            evOwnerService.deleteEvOwner(evOwnerId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

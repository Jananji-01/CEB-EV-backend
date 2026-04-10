package com.example.EVProject.controllers;

import com.example.EVProject.dto.MonthlyConsumptionRequest;
import com.example.EVProject.dto.MonthlyConsumptionResponse;
import com.example.EVProject.services.MonthlyConsumptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/consumption")
public class ConsumptionController {

    private final MonthlyConsumptionService service;

    public ConsumptionController(MonthlyConsumptionService service) {
        this.service = service;
    }

    @PostMapping("/monthly")
    public ResponseEntity<MonthlyConsumptionResponse> monthly(@RequestBody MonthlyConsumptionRequest req) {
        return ResponseEntity.ok(service.calculateAndStore(req));
    }
}

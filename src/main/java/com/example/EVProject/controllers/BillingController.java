package com.example.EVProject.controllers;

import com.example.EVProject.services.BillingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    @Autowired
    private BillingService billingService;

    @PostMapping("/send-to-billing")
    public ResponseEntity<?> sendToBilling(@RequestBody Map<String, Integer> request) {
        Integer transactionId = request.get("transactionId");
        if (transactionId == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Missing transactionId",
                "payload", ""
            ));
        }

        Map<String, Object> result = billingService.sendChargingDataToBilling(transactionId);
        return ResponseEntity.ok(result);
    }
}
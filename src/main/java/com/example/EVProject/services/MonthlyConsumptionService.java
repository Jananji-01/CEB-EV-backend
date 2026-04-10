package com.example.EVProject.services;

import com.example.EVProject.dto.MonthlyConsumptionRequest;
import com.example.EVProject.dto.MonthlyConsumptionResponse;
import com.example.EVProject.model.MonthlyConsumption;
import com.example.EVProject.model.SmartPlug;
import com.example.EVProject.repositories.ChargingSessionRepository;
import com.example.EVProject.repositories.MonthlyConsumptionRepository;
import com.example.EVProject.repositories.SmartPlugRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class MonthlyConsumptionService {

    private final MonthlyConsumptionRepository monthlyRepo;
    private final SmartPlugRepository smartPlugRepo;
    private final ChargingSessionRepository sessionRepo;

    public MonthlyConsumptionService(MonthlyConsumptionRepository monthlyRepo,
                                     SmartPlugRepository smartPlugRepo,
                                     ChargingSessionRepository sessionRepo) {
        this.monthlyRepo = monthlyRepo;
        this.smartPlugRepo = smartPlugRepo;
        this.sessionRepo = sessionRepo;
    }

    public MonthlyConsumptionResponse calculateAndStore(MonthlyConsumptionRequest req) {
        System.out.println("=== SERVICE METHOD STARTED ===");
        
        String username = req.getUsername();
        String eAccountNumber = req.getEAccountNumber();

        if (username == null || eAccountNumber == null) {
            throw new IllegalArgumentException("Username and account number are required");
        }

        LocalDate now = LocalDate.now();
        int month = (req.getMonth() != null) ? req.getMonth() : now.getMonthValue();
        int year = (req.getYear() != null) ? req.getYear() : now.getYear();

        System.out.println("Querying for: account=" + eAccountNumber + ", month=" + month + ", year=" + year);

        Integer totalSessions = null;
        Double totalConsumption = null;
        Double durationDouble = null;
        Double totalAmount = null;
        
        // Test query 1
        try {
            System.out.println("1. Executing countMonthlySessionsByOwner...");
            totalSessions = sessionRepo.countMonthlySessionsByOwner(eAccountNumber, month, year);
            System.out.println("   ✓ Result: " + totalSessions);
        } catch (Exception e) {
            System.err.println("   ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed at count query", e);
        }
        
        // Test query 2
        try {
            System.out.println("2. Executing sumMonthlyConsumptionByOwner...");
            totalConsumption = sessionRepo.sumMonthlyConsumptionByOwner(eAccountNumber, month, year);
            System.out.println("   ✓ Result: " + totalConsumption);
        } catch (Exception e) {
            System.err.println("   ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed at consumption query", e);
        }
        
        // Test query 3 - This is likely the problem
        try {
            System.out.println("3. Executing sumMonthlyDurationMinutesByOwner...");
            durationDouble = sessionRepo.sumMonthlyDurationMinutesByOwner(eAccountNumber, month, year);
            System.out.println("   ✓ Result: " + durationDouble);
        } catch (Exception e) {
            System.err.println("   ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed at duration query", e);
        }
        
        // Test query 4
        try {
            System.out.println("4. Executing sumMonthlyAmountByOwner...");
            totalAmount = sessionRepo.sumMonthlyAmountByOwner(eAccountNumber, month, year);
            System.out.println("   ✓ Result: " + totalAmount);
        } catch (Exception e) {
            System.err.println("   ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed at amount query", e);
        }

        if (totalSessions == null) totalSessions = 0;
        if (totalConsumption == null) totalConsumption = 0.0;
        if (totalAmount == null) totalAmount = 0.0;
        int totalDurationMinutes = (durationDouble == null) ? 0 : (int) Math.round(durationDouble);
        totalConsumption = round3(totalConsumption);

        System.out.println("=== ALL QUERIES SUCCESSFUL ===");
        
        return MonthlyConsumptionResponse.builder()
                .username(username)
                .eAccountNumber(eAccountNumber)
                .idDevice("ALL")
                .month(month)
                .year(year)
                .totalConsumption(totalConsumption)
                .totalSessions(totalSessions)
                .totalDurationMinutes(totalDurationMinutes)
                .totalAmount(totalAmount)
                .build();
    }

    private double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}

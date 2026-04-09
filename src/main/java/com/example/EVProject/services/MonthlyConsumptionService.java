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
        String username = req.getUsername();
        String eAccountNumber = req.getEAccountNumber(); // this is e_account_number

        if (username == null || eAccountNumber == null) {
            throw new IllegalArgumentException("Username and account number are required");
        }

        LocalDate now = LocalDate.now();
        int month = (req.getMonth() != null) ? req.getMonth() : now.getMonthValue();
        int year  = (req.getYear()  != null) ? req.getYear()  : now.getYear();

        // Aggregate from charging_sessions using ev_owner_account_no
        Integer totalSessions = sessionRepo.countMonthlySessionsByOwner(eAccountNumber, month, year);
        Double totalConsumption = sessionRepo.sumMonthlyConsumptionByOwner(eAccountNumber, month, year);
        Double durationDouble = sessionRepo.sumMonthlyDurationMinutesByOwner(eAccountNumber, month, year);
        Double totalAmount = sessionRepo.sumMonthlyAmountByOwner(eAccountNumber, month, year);

        if (totalSessions == null) totalSessions = 0;
        if (totalConsumption == null) totalConsumption = 0.0;
        if (totalAmount == null) totalAmount = 0.0;
        int totalDurationMinutes = (durationDouble == null) ? 0 : (int) Math.round(durationDouble);
        totalConsumption = round3(totalConsumption);

        // Return response (storage in monthly_consumption is skipped for now)
        return MonthlyConsumptionResponse.builder()
                .username(username)
                .eAccountNumber(eAccountNumber)
                .idDevice("ALL") // placeholder, not used by frontend
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
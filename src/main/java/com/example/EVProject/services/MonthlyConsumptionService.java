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
        String accountNo = req.getAccountNumber();

        LocalDate now = LocalDate.now();
        int month = (req.getMonth() != null) ? req.getMonth() : now.getMonthValue();
        int year  = (req.getYear()  != null) ? req.getYear()  : now.getYear();

        // accountNumber == smart_plug.ceb_serial_no
        SmartPlug plug = smartPlugRepo.findFirstByCebSerialNo(accountNo)
                .orElseThrow(() -> new RuntimeException(
                        "No device found for this CEB serial/account number: " + accountNo));

        String deviceId = plug.getIdDevice();

        // ✅ Calculate from charging_sessions (no username column there)
        Integer totalSessions = sessionRepo.countMonthlySessions(deviceId, month, year);
        Double totalConsumption = sessionRepo.sumMonthlyConsumption(deviceId, month, year);
        Double durationDouble = sessionRepo.sumMonthlyDurationMinutes(deviceId, month, year);

        if (totalSessions == null) totalSessions = 0;
        if (totalConsumption == null) totalConsumption = 0.0;

        int totalDurationMinutes = (durationDouble == null) ? 0 : (int) Math.round(durationDouble);

        totalConsumption = round3(totalConsumption);

        // ✅ Upsert into monthly_consumption
        MonthlyConsumption row = monthlyRepo
                .findByUsernameAndIdDeviceAndMonthAndYear(username, deviceId, month, year)
                .orElse(MonthlyConsumption.builder()
                        .username(username)
                        .eAccountNumber(accountNo)
                        .idDevice(deviceId)
                        .month(month)
                        .year(year)
                        .build());

        // ✅ Save all totals
        row.setTotalSessions(totalSessions);
        row.setTotalConsumption(totalConsumption);
        row.setTotalDurationMinutes(totalDurationMinutes);

        System.out.println("DEBUG username=" + username);
        System.out.println("DEBUG accountNo=" + accountNo);
        System.out.println("DEBUG deviceId=" + deviceId);
        System.out.println("DEBUG month/year=" + month + "/" + year);
        System.out.println("DEBUG sessions=" + totalSessions + " cons=" + totalConsumption + " dur=" + totalDurationMinutes);

        monthlyRepo.save(row);

        // ✅ Response
        return MonthlyConsumptionResponse.builder()
                .username(username)
                .accountNumber(accountNo)
                .idDevice(deviceId)
                .month(month)
                .year(year)
                .totalConsumption(totalConsumption)
                .totalSessions(totalSessions)
                .totalDurationMinutes(totalDurationMinutes)
                .build();
    }

    private double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}

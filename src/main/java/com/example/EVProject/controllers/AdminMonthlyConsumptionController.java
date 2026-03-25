package com.example.EVProject.controllers;

import com.example.EVProject.model.MonthlyConsumption;
import com.example.EVProject.repositories.MonthlyConsumptionRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/admin/monthly-consumption")
@CrossOrigin(origins = "*")
public class AdminMonthlyConsumptionController {

    private final MonthlyConsumptionRepository repo;

    public AdminMonthlyConsumptionController(MonthlyConsumptionRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv() {

        List<MonthlyConsumption> rows = repo.findAllByOrderByYearDescMonthDesc();

        StringBuilder sb = new StringBuilder();
        sb.append("username,e_account_number,id_device,month,year,total_sessions,total_consumption,total_duration_minutes,created_at\n");

        for (MonthlyConsumption r : rows) {
            sb.append(safe(r.getUsername())).append(",")
                    .append(safe(r.getEAccountNumber())).append(",")
                    .append(safe(r.getIdDevice())).append(",")
                    .append(r.getMonth()).append(",")
                    .append(r.getYear()).append(",")
                    .append(val(r.getTotalSessions())).append(",")
                    .append(valD(r.getTotalConsumption())).append(",")
                    .append(val(r.getTotalDurationMinutes())).append(",")
                    .append(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                    .append("\n");
        }

        byte[] csv = sb.toString().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=monthly_consumption.csv")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csv);
    }

    private String safe(String s) {
        return s == null ? "" : s.replace(",", " ");
    }
    private String val(Integer v) { return v == null ? "0" : v.toString(); }
    private String valD(Double v) { return v == null ? "0.000" : String.format("%.3f", v); }
}

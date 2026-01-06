package com.example.EVProject.controllers;

import com.example.EVProject.dto.AdminSolarOwnerRowDTO;
import com.example.EVProject.services.AdminSolarOwnerService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admins")
@CrossOrigin
public class AdminSolarOwnerController {

    private final AdminSolarOwnerService service;

    public AdminSolarOwnerController(AdminSolarOwnerService service) {
        this.service = service;
    }

    @GetMapping("/solar-owners")
    public List<AdminSolarOwnerRowDTO> getSolarOwners(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String accountNo,
            @RequestParam(required = false) String username
    ) {
        LocalDate now = LocalDate.now();
        int m = (month == null) ? now.getMonthValue() : month;
        int y = (year == null) ? now.getYear() : year;

        return service.getRows(m, y, accountNo, username);
    }

    @GetMapping("/solar-owners/csv")
    public ResponseEntity<byte[]> downloadSolarOwnersCsv(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String accountNo,
            @RequestParam(required = false) String username
    ) {
        LocalDate now = LocalDate.now();
        int m = (month == null) ? now.getMonthValue() : month;
        int y = (year == null) ? now.getYear() : year;

        List<AdminSolarOwnerRowDTO> rows = service.getRows(m, y, accountNo, username);

        StringBuilder sb = new StringBuilder();
        sb.append("Account No,Username,Monthly kWh,Email,Contact\n");
        for (AdminSolarOwnerRowDTO r : rows) {
            sb.append(csv(r.getAccountNo())).append(",")
                    .append(csv(r.getUsername())).append(",")
                    .append(r.getTotalKwh() == null ? 0 : r.getTotalKwh()).append(",")
                    .append(csv(r.getEmail())).append(",")
                    .append(csv(r.getContactNo())).append("\n");
        }

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=solar_owners.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    private String csv(String s) {
        if (s == null) return "";
        String v = s.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\n")) return "\"" + v + "\"";
        return v;
    }
}

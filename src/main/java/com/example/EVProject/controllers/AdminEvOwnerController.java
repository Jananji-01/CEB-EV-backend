package com.example.EVProject.controllers;

import com.example.EVProject.dto.AdminEvOwnerBasicDTO;
import com.example.EVProject.services.AdminEvOwnerService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/admins")
@CrossOrigin
public class AdminEvOwnerController {

    private final AdminEvOwnerService service;

    public AdminEvOwnerController(AdminEvOwnerService service) {
        this.service = service;
    }

    // JSON list
    @GetMapping("/ev-owners")
    public List<AdminEvOwnerBasicDTO> getEvOwners(
            @RequestParam(required = false) String accountNo,
            @RequestParam(required = false) String username
    ) {
        return service.getEvOwners(accountNo, username);
    }

    // CSV download
    @GetMapping("/ev-owners/csv")
    public ResponseEntity<byte[]> downloadCsv(
            @RequestParam(required = false) String accountNo,
            @RequestParam(required = false) String username
    ) {
        List<AdminEvOwnerBasicDTO> rows = service.getEvOwners(accountNo, username);

        StringBuilder sb = new StringBuilder();
        sb.append("Account No,Username,Email,Contact,No Of Vehicles\n");
        for (AdminEvOwnerBasicDTO r : rows) {
            sb.append(csv(r.getAccountNo())).append(",")
                    .append(csv(r.getUsername())).append(",")
                    .append(csv(r.getEmail())).append(",")
                    .append(csv(r.getContactNo())).append(",")
                    .append(r.getNoOfVehiclesOwned()).append("\n");
        }

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ev_owners.csv")
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

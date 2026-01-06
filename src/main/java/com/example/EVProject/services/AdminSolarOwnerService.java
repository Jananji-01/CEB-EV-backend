package com.example.EVProject.services;

import com.example.EVProject.dto.AdminSolarOwnerRowDTO;
import com.example.EVProject.repositories.RooftopSolarOwnerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminSolarOwnerService {

    private final RooftopSolarOwnerRepository repo;

    public AdminSolarOwnerService(RooftopSolarOwnerRepository repo) {
        this.repo = repo;
    }

    public List<AdminSolarOwnerRowDTO> getRows(int month, int year, String accountNo, String username) {
        String acc = (accountNo == null || accountNo.trim().isEmpty()) ? null : accountNo.trim();
        String user = (username == null || username.trim().isEmpty()) ? null : username.trim();

        return repo.getSolarOwnerRowsRaw(month, year, acc, user).stream().map(r -> {
            String accNo = (String) r[0];
            String uname = (String) r[1];
            Double totalKwh = (r[2] == null) ? 0.0 : ((Number) r[2]).doubleValue();
            String email = (String) r[3];
            String contact = (String) r[4];

            return new AdminSolarOwnerRowDTO(accNo, uname, totalKwh, email, contact);
        }).toList();
    }
}

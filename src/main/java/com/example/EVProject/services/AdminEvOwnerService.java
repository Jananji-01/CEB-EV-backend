package com.example.EVProject.services;

import com.example.EVProject.dto.AdminEvOwnerBasicDTO;
import com.example.EVProject.repositories.EvOwnerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminEvOwnerService {

    private final EvOwnerRepository repo;

    public AdminEvOwnerService(EvOwnerRepository repo) {
        this.repo = repo;
    }

    public List<AdminEvOwnerBasicDTO> getEvOwners(String accountNo, String username) {
        String acc = (accountNo == null || accountNo.trim().isEmpty()) ? null : accountNo.trim();
        String user = (username == null || username.trim().isEmpty()) ? null : username.trim();

        return repo.findAdminEvOwnersRaw(acc, user).stream().map(r -> {
            String accNo = (String) r[0];
            String uname = (String) r[1];
            String email = (String) r[2];
            String contact = (String) r[3];
            Integer vehicles = (r[4] == null) ? 0 : ((Number) r[4]).intValue();

            return new AdminEvOwnerBasicDTO(accNo, uname, email, contact, vehicles);
        }).toList();
    }
}

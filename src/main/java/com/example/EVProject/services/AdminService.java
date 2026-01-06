package com.example.EVProject.services;

import com.example.EVProject.dto.AdminDTO;
import com.example.EVProject.dto.RegistrationRequest;
import com.example.EVProject.dto.UserDTO;
import com.example.EVProject.model.Admin;
import com.example.EVProject.model.User;
import com.example.EVProject.repositories.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private UserService userService;

    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    public Optional<Admin> getAdminById(Integer adminId) {
        return adminRepository.findById(adminId);
    }

    public Optional<Admin> getAdminByUsername(String username) {
        return adminRepository.findByUsername(username);
    }

    public Admin createAdmin(AdminDTO adminDTO) {
        if (adminRepository.existsByUsername(adminDTO.getUsername())) {
            throw new RuntimeException("Admin with this username already exists");
        }

        // Create user first
        User user = userService.createUser(convertToUserDTO(adminDTO));

        // Create admin
        Admin admin = new Admin();
        admin.setUsername(adminDTO.getUsername());
        admin.setUser(user);

        return adminRepository.save(admin);
    }

    public Admin updateAdmin(Integer adminId, AdminDTO adminDTO) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        // Update user information
        userService.updateUser(admin.getUsername(), convertToUserDTO(adminDTO));

        return admin;
    }

    public void deleteAdmin(Integer adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        String username = admin.getUsername();
        adminRepository.deleteById(adminId);
        userService.deleteUser(username);
    }

    private RegistrationRequest convertToUserDTO(AdminDTO adminDTO) {
        RegistrationRequest userDTO = new RegistrationRequest();
        //UserDTO userDTO = new UserDTO(req.getUsername(), req.getEmail(), req.getPassword());
        userDTO.setUsername(adminDTO.getUsername());
        userDTO.setEmail(adminDTO.getEmail());
        userDTO.setPassword(adminDTO.getPassword());
        return userDTO;
    }
}

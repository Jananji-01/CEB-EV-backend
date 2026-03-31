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

    // Return minimal data
    public List<AdminDTO> getAllAdminsWithEmail() {
        return adminRepository.findAll().stream()
                .map(admin -> {
                    AdminDTO dto = new AdminDTO();
                    dto.setAdminId(admin.getAdminId());
                    dto.setUsername(admin.getUsername());
                    if(admin.getUser() != null) {
                        dto.setEmail(admin.getUser().getEmail());
                    }
                    return dto;
                })
                .toList();
    }

    public Optional<AdminDTO> getAdminByIdWithEmail(Integer adminId) {
        return adminRepository.findById(adminId)
                .map(admin -> {
                    AdminDTO dto = new AdminDTO();
                    dto.setAdminId(admin.getAdminId());
                    dto.setUsername(admin.getUsername());
                    if(admin.getUser() != null) {
                        dto.setEmail(admin.getUser().getEmail()); // <-- includes email
                    }
                    return dto;
                });
    }

    public Optional<Admin> getAdminById(Integer adminId) {
        return adminRepository.findById(adminId);
    }

    public Optional<Admin> getAdminByUsername(String username) {
        return adminRepository.findByUsername(username);
    }

//    public Admin createAdmin(AdminDTO adminDTO) {
//        if (adminRepository.existsByUsername(adminDTO.getUsername())) {
//            throw new RuntimeException("Admin with this username already exists");
//        }
//
//        // Create user first
//        User user = userService.createUser(convertToUserDTO(adminDTO));
//
//        // Create admin
//        Admin admin = new Admin();
//        admin.setUsername(adminDTO.getUsername());
//        admin.setUser(user);
//
//        return adminRepository.save(admin);
//    }

    //create admin
    public Admin createAdmin(AdminDTO dto) {

        // 1️⃣ Create user in APP_USER
        RegistrationRequest req = new RegistrationRequest();
        req.setUsername(dto.getUsername());
        req.setEmail(dto.getEmail());
        req.setPassword(dto.getPassword());
        req.setRole("ADMIN");

        User user = userService.createAdminUser(req);

        // 2️⃣ Insert into ADMIN table
        Admin admin = new Admin();
        admin.setUsername(user.getUsername());

        return adminRepository.save(admin);
    }

    public Admin updateAdmin(Integer adminId, AdminDTO adminDTO) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        String originalUsername = admin.getUsername();

        if (adminDTO.getPassword() == null || adminDTO.getPassword().isBlank()) {
            throw new RuntimeException("Password is required for update");
        }

        // Update User (password & email required)
        RegistrationRequest req = new RegistrationRequest();
        req.setUsername(originalUsername); // username in User table cannot change
        req.setEmail(adminDTO.getEmail());
        req.setPassword(adminDTO.getPassword()); // required

        userService.updateUser(originalUsername, req);

        // Update Admin username if changed
        if (!originalUsername.equals(adminDTO.getUsername())) {
            admin.setUsername(adminDTO.getUsername());
        }

        return adminRepository.save(admin);
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

package com.example.EVProject.controllers;

import com.example.EVProject.dto.AdminDTO;
import com.example.EVProject.model.Admin;
import com.example.EVProject.security.JwtUtil;
import com.example.EVProject.services.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.*;

@RestController
@RequestMapping("/api/admins")
//@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AdminService adminService;

    @GetMapping("/dashboard")
    public String dashboard() {
        return "Welcome to ADMIN Dashboard";
    }

    @GetMapping
    public ResponseEntity<List<AdminDTO>> getAllAdmins() {
        return ResponseEntity.ok(adminService.getAllAdminsWithEmail());
    }

    @GetMapping("/{adminId}")
    public ResponseEntity<AdminDTO> getAdminById(@PathVariable Integer adminId) {
        return adminService.getAdminByIdWithEmail(adminId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


//    @PostMapping
//    public ResponseEntity<Admin> createAdmin(@Valid @RequestBody AdminDTO adminDTO) {
//        try {
//            Admin admin = adminService.createAdmin(adminDTO);
//            return ResponseEntity.ok(admin);
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().build();
//        }
//    }

    //create admin
    @PostMapping
    public ResponseEntity<?> createAdmin(@Valid @RequestBody AdminDTO adminDTO) {
        try {
            Admin admin = adminService.createAdmin(adminDTO);

            // ✅ Set roles for JWT
            Set<String> roles = new HashSet<>();
            roles.add("ROLE_ADMIN");

            // ✅ Generate token
            String token = jwtUtil.generateToken(admin.getUsername(), roles);

            return ResponseEntity.ok().body(
                    Map.of(
                            "message", "Admin created successfully",
                            "token", token
                    )
            );

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{adminId}")
    public ResponseEntity<AdminDTO> updateAdmin(
            @PathVariable Integer adminId,
            @Valid @RequestBody AdminDTO adminDTO) {
        try {
            Admin updatedAdmin = adminService.updateAdmin(adminId, adminDTO);

            AdminDTO responseDTO = new AdminDTO();
            responseDTO.setAdminId(updatedAdmin.getAdminId());
            responseDTO.setUsername(updatedAdmin.getUsername());
            if (updatedAdmin.getUser() != null) {
                responseDTO.setEmail(updatedAdmin.getUser().getEmail());
            }

            // Do not include password
            responseDTO.setPassword(null);

            return ResponseEntity.ok(responseDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/{adminId}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Integer adminId) {
        try {
            adminService.deleteAdmin(adminId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/test")
    public String test() {
        return "Admin API working";
    }
}

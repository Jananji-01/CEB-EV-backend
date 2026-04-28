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
    public ResponseEntity<?> updateAdmin(
            @PathVariable Integer adminId,
            @Valid @RequestBody AdminDTO adminDTO) {
        try {
            Admin updatedAdmin = adminService.updateAdmin(adminId, adminDTO);
            
            // Create response without password
            Map<String, Object> response = new HashMap<>();
            response.put("adminId", updatedAdmin.getAdminId());
            response.put("username", updatedAdmin.getUsername());
            if (updatedAdmin.getUser() != null) {
                response.put("email", updatedAdmin.getUser().getEmail());
            }
            response.put("message", "Admin updated successfully");
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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

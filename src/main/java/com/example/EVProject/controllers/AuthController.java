package com.example.EVProject.controllers;

import com.example.EVProject.dto.*;
import com.example.EVProject.model.User;
import com.example.EVProject.repositories.UserRepository;
import com.example.EVProject.security.JwtUtil;
import com.example.EVProject.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          UserRepository userRepository) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegistrationRequest req) {
        try {
            RegistrationRequest dto = new RegistrationRequest();
            dto.setUsername(req.getUsername());
            dto.setEmail(req.getEmail());
            dto.setE_account_number(req.getE_account_number());
            dto.setPassword(req.getPassword());
            dto.setRole(req.getRole());

            userService.createUser(dto);

            return ResponseEntity.ok(new ApiResponse(true, "Registered. OTP sent to email."));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Registration failed"));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest req) {
        try {
            boolean ok = userService.verifyOtp(req.getUsername(), req.getOtp()); // ✅ uses Redis OTP
            if (ok) {
                return ResponseEntity.ok(new ApiResponse(true, "Verified. You can login now."));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(false, "Invalid or expired OTP."));
            }
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );

            User user = userRepository.findById(req.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!user.getEnabled()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "Account not verified"));
            }

            // Ensure roles is not null
            Set<String> roles = user.getRoles() != null
                    ? user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet())
                    : Collections.emptySet();

            System.out.println(roles);
            String token = jwtUtil.generateToken(user.getUsername(), roles);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("roles", roles);

            return ResponseEntity.ok(
                    new ApiResponse(
                            true,
                            "Login successful",
                            token,
                            roles,
                            user.getUsername(),
                            user.getEAccountNumber() // ✅ correct getter
                    )
            );




        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Invalid credentials"));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Authentication failed"));
        }
    }


    // Optional resend OTP
    @PostMapping("/resend-otp/{username}")
    public ResponseEntity<ApiResponse> resendOtp(@PathVariable String username) {
        try {
            userService.resendOtp(username); // ✅ uses Redis OTP internally
            return ResponseEntity.ok(new ApiResponse(true, "OTP resent to email"));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, ex.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(false, "No token provided"));
            }

            String token = authHeader.substring(7); // remove "Bearer "

            // ✅ Optional: if you use Redis/DB, you can store blacklisted tokens
            // tokenBlacklistService.addToBlacklist(token);

            return ResponseEntity.ok(new ApiResponse(true, "Logged out successfully"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Logout failed"));
        }
    }

}

package com.example.EVProject.services;

import com.example.EVProject.dto.RegistrationRequest;
import com.example.EVProject.model.EvOwner;
import com.example.EVProject.model.Role;
import com.example.EVProject.model.RooftopSolarOwner;
import com.example.EVProject.model.User;
import com.example.EVProject.repositories.EvOwnerRepository;
import com.example.EVProject.repositories.RoleRepository;
import com.example.EVProject.repositories.RooftopSolarOwnerRepository;
import com.example.EVProject.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EvOwnerRepository evOwnerRepository;

    @Autowired
    private RooftopSolarOwnerRepository rooftopSolarOwnerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    private final int otpExpiryMinutes = 10;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findById(username);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User createUser(RegistrationRequest req) {
        // Use the native query methods
        if (userRepository.existsByUsernameNative(req.getUsername()) > 0) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmailNative(req.getEmail()) > 0) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setEAccountNumber(req.getE_account_number());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setEnabled(false);

        Role roleEntity = roleRepository.findByName("ROLE_" + req.getRole().toUpperCase())
                .orElseThrow(() -> new RuntimeException("Role not found: ROLE_" + req.getRole().toUpperCase()));
        user.setRoles(Collections.singleton(roleEntity));

        System.out.println("ROLE from request = " + req.getRole());

        User savedUser = userRepository.save(user);

        switch (req.getRole().toUpperCase()) {
            case "EVOWNER":
                EvOwner evOwner = new EvOwner();
                evOwner.setUsername(savedUser.getUsername());
                evOwner.setEAccountNumber(req.getE_account_number());
                evOwner.setMobileNumber(req.getMobile_number());
                // Fix: Use the correct getter name with underscores
                evOwner.setNoOfVehiclesOwned(req.getNo_of_vehicles_owned());
                // Generate unique ID tag
                String idTag = "IDT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                evOwner.setIdTag(idTag);
                evOwnerRepository.save(evOwner);
                break;
            case "SOLAROWNER":
                RooftopSolarOwner solarOwner = new RooftopSolarOwner();
                solarOwner.setAddress(req.getAddress());
                solarOwner.setEAccountNumber(req.getE_account_number());
                solarOwner.setMobileNumber(req.getMobile_number());
                solarOwner.setPanelCapacity(req.getSolarCapacity());
                solarOwner.setUsername(savedUser.getUsername());
                rooftopSolarOwnerRepository.save(solarOwner);
                break;
            default:
                break;
        }

        generateAndSendOtp(savedUser);
        return savedUser;
    }

    public User updateUser(String username, RegistrationRequest userDTO) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update email (check uniqueness)
        if (!user.getEmail().equals(userDTO.getEmail()) &&
                userRepository.existsByEmailNative(userDTO.getEmail()) > 0) {
            throw new RuntimeException("Email already exists");
        }
        user.setEmail(userDTO.getEmail());

        // ✅ Password is required → always encode & update
        if (userDTO.getPassword() == null || userDTO.getPassword().isBlank()) {
            throw new RuntimeException("Password is required");
        }
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        return userRepository.save(user);
    }

    public void deleteUser(String username) {
        if (!userRepository.existsById(username)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(username);
    }

    public boolean authenticateUser(String username, String password) {
        Optional<User> user = userRepository.findById(username);
        return user.isPresent()
                && user.get().getEnabled()
                && passwordEncoder.matches(password, user.get().getPassword());
    }

    // ========== OTP methods ==========

    private void generateAndSendOtp(User user) {
        String otp = generateOtp();
        user.setLastOtp(otp);
        // Store expiry in UTC
        user.setOtpExpiry(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(otpExpiryMinutes));
        userRepository.save(user);
        emailService.sendOtpEmail(user.getEmail(), otp);
    }

    private String generateOtp() {
        Random rnd = new Random();
        int num = 100000 + rnd.nextInt(900000);
        return String.valueOf(num);
    }

    public boolean verifyOtp(String username, String otp) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getLastOtp() == null || user.getOtpExpiry() == null) {
            return false;
        }
        if (!user.getLastOtp().equals(otp)) {
            return false;
        }
        // Compare with current UTC time
        if (user.getOtpExpiry().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            return false;
        }
        user.setEnabled(true);
        userRepository.save(user);
        emailService.sendWelcomeEmail(user.getEmail());
        return true;
    }

    public void resendOtp(String username) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        generateAndSendOtp(user);
    }

    //create admin
    public User createAdminUser(RegistrationRequest req) {

        if (userRepository.existsByUsernameNative(req.getUsername()) > 0) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmailNative(req.getEmail()) > 0) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));

        // ✅ NO OTP → directly enable
        user.setEnabled(true);

        // ✅ Assign ADMIN role
        Role roleEntity = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("Role not found"));

        user.setRoles(Collections.singleton(roleEntity));

        return userRepository.save(user);
    }

    public Map<String, Long> getUserRoleCounts() {
        List<Object[]> results = userRepository.countUsersByRole();

        long admins = 0, evOwners = 0, solarOwners = 0;

        for (Object[] row : results) {
            String roleName = (String) row[0];
            long count = (Long) row[1];

            if (roleName.equals("ROLE_ADMIN")) admins = count;
            else if (roleName.equals("ROLE_EVOWNER")) evOwners = count;
            else if (roleName.equals("ROLE_SOLAROWNER")) solarOwners = count;
        }

        Map<String, Long> map = new HashMap<>();
        map.put("admins", admins);
        map.put("evOwners", evOwners);
        map.put("solarOwners", solarOwners);

        return map;
    }
}
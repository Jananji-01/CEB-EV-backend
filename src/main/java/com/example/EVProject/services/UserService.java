////package com.example.EVProject.services;
////
////import com.example.EVProject.dto.UserDTO;
////import com.example.EVProject.model.User;
////import com.example.EVProject.repositories.UserRepository;
////import org.springframework.beans.factory.annotation.Autowired;
////import org.springframework.security.crypto.password.PasswordEncoder;
////import org.springframework.stereotype.Service;
////import org.springframework.transaction.annotation.Transactional;
////
////import java.util.List;
////import java.util.Optional;
////
////@Service
////@Transactional
////public class UserService {
////
////    @Autowired
////    private UserRepository userRepository;
////
////    @Autowired
////    private PasswordEncoder passwordEncoder;
////
////    public List<User> getAllUsers() {
////        return userRepository.findAll();
////    }
////
////    public Optional<User> getUserByUsername(String username) {
////        return userRepository.findById(username);
////    }
////
////    public Optional<User> getUserByEmail(String email) {
////        return userRepository.findByEmail(email);
////    }
////
////    public User createUser(UserDTO userDTO) {
////        if (userRepository.existsByUsername(userDTO.getUsername())) {
////            throw new RuntimeException("Username already exists");
////        }
////        if (userRepository.existsByEmail(userDTO.getEmail())) {
////            throw new RuntimeException("Email already exists");
////        }
////
////        User user = new User();
////        user.setUsername(userDTO.getUsername());
////        user.setEmail(userDTO.getEmail());
////        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
////
////        return userRepository.save(user);
////    }
////
////    public User updateUser(String username, UserDTO userDTO) {
////        User user = userRepository.findById(username)
////                .orElseThrow(() -> new RuntimeException("User not found"));
////
////        if (!user.getEmail().equals(userDTO.getEmail()) &&
////                userRepository.existsByEmail(userDTO.getEmail())) {
////            throw new RuntimeException("Email already exists");
////        }
////
////        user.setEmail(userDTO.getEmail());
////        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
////            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
////        }
////
////        return userRepository.save(user);
////    }
////
////    public void deleteUser(String username) {
////        if (!userRepository.existsById(username)) {
////            throw new RuntimeException("User not found");
////        }
////        userRepository.deleteById(username);
////    }
////
////    public boolean authenticateUser(String username, String password) {
////        Optional<User> user = userRepository.findById(username);
////        return user.isPresent() && passwordEncoder.matches(password, user.get().getPassword());
////    }
////}
//
//package com.example.EVProject.services;
//
//import com.example.EVProject.dto.RegistrationRequest;
//import com.example.EVProject.model.EvOwner;
//import com.example.EVProject.model.Role;
//import com.example.EVProject.model.RooftopSolarOwner;
//import com.example.EVProject.model.User;
//import com.example.EVProject.repositories.EvOwnerRepository;
//import com.example.EVProject.repositories.RoleRepository;
//import com.example.EVProject.repositories.RooftopSolarOwnerRepository;
//import com.example.EVProject.repositories.UserRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.OffsetDateTime;
//import java.time.ZoneOffset;
//import java.util.Collections;
//import java.util.List;
//import java.util.Optional;
//import java.util.Random;
//import java.util.concurrent.TimeUnit;
//
//
//@Service
//@Transactional
//public class UserService {
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private RoleRepository roleRepository;
//
//    @Autowired
//    private EvOwnerRepository evOwnerRepository;
//
//    @Autowired
//    private RooftopSolarOwnerRepository rooftopSolarOwnerRepository;
//
//    @Autowired
//    private PasswordEncoder passwordEncoder;
//
//    @Autowired
//    private EmailService emailService; // ✅ Email sender service
//
//   // @Autowired
//    //private RedisTemplate<String, String> redisTemplate; // ✅ Redis for OTP
//
////    @Autowired
////    private RedisOtpService redisOtpService;
//
//    private final int otpExpiryMinutes = 10; // OTP expiry time
//
//    public List<User> getAllUsers() {
//        return userRepository.findAll();
//    }
//
//    public Optional<User> getUserByUsername(String username) {
//        return userRepository.findById(username);
//    }
//
//    public Optional<User> getUserByEmail(String email) {
//        return userRepository.findByEmail(email);
//    }
//
//    public User createUser(RegistrationRequest req) {
//        // Check if username or email exists
//        if (userRepository.existsByUsername(req.getUsername())) {
//            throw new RuntimeException("Username already exists");
//        }
//        if (userRepository.existsByEmail(req.getEmail())) {
//            throw new RuntimeException("Email already exists");
//        }
//
//        // 1️⃣ Create User entity
//        User user = new User();
//        user.setUsername(req.getUsername());
//        user.setEmail(req.getEmail());
//        user.setEAccountNumber(req.getE_account_number());
//        user.setPassword(passwordEncoder.encode(req.getPassword()));
//        user.setEnabled(false); // Until OTP verified
//
//        // Assign role
//        Role roleEntity = roleRepository.findByName("ROLE_" + req.getRole().toUpperCase())
//                .orElseThrow(() -> new RuntimeException("Role not found"));
//        user.setRoles(Collections.singleton(roleEntity));
//
//        System.out.println("ROLE from request = " + req.getRole());
//
//        User savedUser = userRepository.save(user);
//
//        // 2️⃣ Handle role-specific tables
//        switch (req.getRole().toUpperCase()) {
//            case "EVOWNER":
//                EvOwner evOwner = new EvOwner();
//                evOwner.setUsername(savedUser.getUsername());
//                evOwner.setEAccountNumber(req.getE_account_number());
//                evOwner.setMobileNumber(req.getMobile_number());
//                evOwner.setNoOfVehiclesOwned(req.getNo_of_vehicles_owned());
//                evOwnerRepository.save(evOwner);
//                break;
//
//            case "SOLAROWNER":
//                RooftopSolarOwner solarOwner = new RooftopSolarOwner();
//                solarOwner.setAddress(req.getAddress());
//                solarOwner.setEAccountNumber(req.getE_account_number());
//                solarOwner.setMobileNumber(req.getMobile_number());
//                solarOwner.setPanelCapacity(req.getSolarCapacity());
//                solarOwner.setUsername(savedUser.getUsername());
//
//                rooftopSolarOwnerRepository.save(solarOwner);
//                break;
//
//            // USER or ADMIN: no extra table insert
//            default:
//                break;
//        }
//
//
//        // 3️⃣ Generate OTP and send email
//        generateAndSendOtp(savedUser);
//
//        return savedUser;
//    }
//
//
////    private void generateAndSendOtpRedis(User user) {
////        // Generate 6-digit OTP
////        Random rnd = new Random();
////        int num = 100000 + rnd.nextInt(900000);
////        String otp = String.valueOf(num);
////
////        // Store OTP in Redis with expiry
////        redisOtpService.storeOtp(user.getUsername(), otp, otpExpiryMinutes);
////
////        // Send OTP email
////        emailService.sendOtpEmail(user.getEmail(), otp);
////    }
//
//    public User updateUser(String username, RegistrationRequest userDTO) {
//        User user = userRepository.findById(username)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        if (!user.getEmail().equals(userDTO.getEmail()) &&
//                userRepository.existsByEmail(userDTO.getEmail())) {
//            throw new RuntimeException("Email already exists");
//        }
//
//        user.setEmail(userDTO.getEmail());
//        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
//            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
//        }
//
//        return userRepository.save(user);
//    }
//
//    public void deleteUser(String username) {
//        if (!userRepository.existsById(username)) {
//            throw new RuntimeException("User not found");
//        }
//        userRepository.deleteById(username);
//    }
//
//    public boolean authenticateUser(String username, String password) {
//        Optional<User> user = userRepository.findById(username);
//        return user.isPresent()
//                && user.get().getEnabled() // only verified accounts
//                && passwordEncoder.matches(password, user.get().getPassword());
//    }
//
//    // ==============================
//    // 🔑 OTP methods (Redis version)
//    // ==============================
//
//    private void generateAndSendOtp(User user) {
//        String otp = generateOtp();
//        //String key = "OTP:" + user.getUsername();
//        user.setLastOtp(otp);
//        user.setOtpExpiry(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(otpExpiryMinutes));
//        // store OTP in Redis with expiry
//        //redisTemplate.opsForValue().set(key, otp, otpExpiryMinutes, TimeUnit.MINUTES);
//
//        userRepository.save(user);
//        // send OTP via email
//        emailService.sendOtpEmail(user.getEmail(), otp);
//    }
//
//    private String generateOtp() {
//        Random rnd = new Random();
//        int num = 100000 + rnd.nextInt(900000); // 6-digit OTP
//        return String.valueOf(num);
//    }
//
//    public boolean verifyOtp(String username, String otp) {
////        String key = "OTP:" + username;
////        String storedOtp = redisTemplate.opsForValue().get(key);
////
//
//        // OTP valid → enable account
//        User user = userRepository.findById(username)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        if (user.getLastOtp() == null || user.getOtpExpiry() == null) {
//            return false;
//        }
//        if (!user.getLastOtp().equals(otp)) {
//            return false;
//        }
//        if (user.getOtpExpiry().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
//            return false;
//        }
//        user.setEnabled(true);
//        userRepository.save(user);
//
////        // remove OTP from Redis (one-time use)
////        redisTemplate.delete(key);
//
//        emailService.sendWelcomeEmail(user.getEmail());
//        return true;
//    }
//
//    public void resendOtp(String username) {
//        User user = userRepository.findById(username)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        generateAndSendOtp(user);
//    }
//}
//
//



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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

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

        if (!user.getEmail().equals(userDTO.getEmail()) &&
                userRepository.existsByEmailNative(userDTO.getEmail()) > 0) {
            throw new RuntimeException("Email already exists");
        }

        user.setEmail(userDTO.getEmail());
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

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
}
//package com.example.EVProject.services;
//
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.util.Random;
//import java.util.concurrent.TimeUnit;
//
//@Service
//public class OtpService {
//
//    private final RedisTemplate<String, String> redisTemplate;
//    private static final int OTP_EXPIRY_MINUTES = 5;
//
//    public OtpService(RedisTemplate<String, String> redisTemplate) {
//        this.redisTemplate = redisTemplate;
//    }
//
//    // Generate OTP and store in Redis
//    public String generateOtp(String email) {
//        String otp = String.valueOf(100000 + new Random().nextInt(900000)); // 6-digit OTP
//        redisTemplate.opsForValue().set("OTP:" + email, otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);
//        return otp;
//    }
//
//    // Validate OTP
//    public boolean validateOtp(String email, String otp) {
//        String key = "OTP:" + email;
//        String storedOtp = redisTemplate.opsForValue().get(key);
//
//        if (storedOtp != null && storedOtp.equals(otp)) {
//            redisTemplate.delete(key); // one-time use
//            return true;
//        }
//        return false;
//    }
//}

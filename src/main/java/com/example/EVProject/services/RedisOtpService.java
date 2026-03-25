//package com.example.EVProject.services;
//
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.time.Duration;
//
//@Service
//public class RedisOtpService {
//
//    private final RedisTemplate<String, String> redisTemplate;
//
//    public RedisOtpService(RedisTemplate<String, String> redisTemplate) {
//        this.redisTemplate = redisTemplate;
//    }
//
//    // Store OTP in Redis with expiry
//    public void storeOtp(String username, String otp, int expiryMinutes) {
//        String key = "OTP:" + username;
//        redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(expiryMinutes));
//    }
//
//    // Get OTP from Redis
//    public String getOtp(String username) {
//        String key = "OTP:" + username;
//        return redisTemplate.opsForValue().get(key);
//    }
//
//    // Delete OTP from Redis
//    public void deleteOtp(String username) {
//        String key = "OTP:" + username;
//        redisTemplate.delete(key);
//    }
//}

package com.example.EVProject.services;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender){
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String to, String otp){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your verification OTP");
        message.setText("Your OTP code is: " + otp + "\n\nThis code will expire shortly.");
        mailSender.send(message);
    }

    // Optionally: email for success
    public void sendWelcomeEmail(String to){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Welcome — account verified");
        message.setText("Your account has been verified. You can now log in.");
        mailSender.send(message);
    }
}

package com.example.EVProject.controllers;

import com.example.EVProject.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class UserCountChart {
    @Autowired
    private UserService userService;

    @GetMapping("/user-role-counts")
    public Map<String, Long> getUserRoleCounts() {
        return userService.getUserRoleCounts();
    }

}

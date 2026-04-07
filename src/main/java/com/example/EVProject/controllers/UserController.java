////package com.example.EVProject.controllers;
////
////import com.example.EVProject.dto.RegistrationRequest;
////import com.example.EVProject.dto.UserDTO;
////import com.example.EVProject.model.User;
////import com.example.EVProject.services.UserService;
////import org.springframework.beans.factory.annotation.Autowired;
////import org.springframework.http.ResponseEntity;
////import org.springframework.web.bind.annotation.*;
////
////import java.util.List;
////import java.util.Optional;
////
////@RestController
////@RequestMapping("/api/users")
//////@CrossOrigin(origins = "*")
////public class UserController {
////
////    @Autowired
////    private UserService userService;
////
////    @GetMapping
////    public ResponseEntity<List<User>> getAllUsers() {
////        List<User> users = userService.getAllUsers();
////        return ResponseEntity.ok(users);
////    }
////
////    @GetMapping("/{username}")
////    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
////        Optional<User> user = userService.getUserByUsername(username);
////        return user.map(ResponseEntity::ok)
////                .orElse(ResponseEntity.notFound().build());
////    }
////
////
////    @GetMapping("/ping")
////    public ResponseEntity<String> ping() {
////        return ResponseEntity.ok("pong");
////    }
////
////
////    @PostMapping
////    public ResponseEntity<User> createUser(@RequestBody RegistrationRequest userDTO) {
////        try {
////            User user = userService.createUser(userDTO);
////            return ResponseEntity.ok(user);
////        } catch (RuntimeException e) {
////            return ResponseEntity.badRequest().build();
////        }
////    }
////
////    @PutMapping("/{username}")
////    public ResponseEntity<User> updateUser(@PathVariable String username,
////                                           @RequestBody RegistrationRequest userDTO) {
////        try {
////            User user = userService.updateUser(username, userDTO);
////            return ResponseEntity.ok(user);
////        } catch (RuntimeException e) {
////            return ResponseEntity.badRequest().build();
////        }
////    }
////
////    @DeleteMapping("/{username}")
////    public ResponseEntity<Void> deleteUser(@PathVariable String username) {
////        try {
////            userService.deleteUser(username);
////            return ResponseEntity.noContent().build();
////        } catch (RuntimeException e) {
////            return ResponseEntity.notFound().build();
////        }
////    }
////
////    @PostMapping("/authenticate")
////    public ResponseEntity<String> authenticateUser(@RequestBody UserDTO userDTO) {
////        boolean authenticated = userService.authenticateUser(userDTO.getUsername(), userDTO.getPassword());
////        if (authenticated) {
////            return ResponseEntity.ok("Authentication successful");
////        } else {
////            return ResponseEntity.status(401).body("Authentication failed");
////        }
////    }
////
////
////
////}
//
//
//
//package com.example.EVProject.controllers;
//
//import com.example.EVProject.dto.RegistrationRequest;
//import com.example.EVProject.dto.UserDTO;
//import com.example.EVProject.model.User;
//import com.example.EVProject.repositories.UserRepository;
//import com.example.EVProject.services.UserService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.Optional;
//
//@RestController
//@RequestMapping("/api/users")
//public class UserController {
//
//    @Autowired
//    private UserService userService;
//
//    @Autowired
//    private UserRepository userRepository; // <-- field injection, class body ok
//
//    // ------------------- Username check endpoint -------------------
//    @GetMapping("/check/{username}")
//    public ResponseEntity<String> checkUserExists(@PathVariable String username) {
//        // ✅ statements INSIDE method, NOT class body
//        Optional<String> userOpt = userRepository.findUsername(username);
//        if(userOpt.isPresent()){
//            return ResponseEntity.ok("User exists: " + userOpt.get());
//        } else {
//            return ResponseEntity.ok("User does not exist");
//        }
//    }
//
//    // ------------------- other endpoints -------------------
//    @GetMapping
//    public ResponseEntity<List<User>> getAllUsers() {
//        return ResponseEntity.ok(userService.getAllUsers());
//    }
//
//    @GetMapping("/{username}")
//    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
//        Optional<User> user = userService.getUserByUsername(username);
//        return user.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
//    }
//
//    @PostMapping
//    public ResponseEntity<User> createUser(@RequestBody RegistrationRequest req) {
//        return ResponseEntity.ok(userService.createUser(req));
//    }
//
//    @PutMapping("/{username}")
//    public ResponseEntity<User> updateUser(@PathVariable String username, @RequestBody RegistrationRequest req) {
//        return ResponseEntity.ok(userService.updateUser(username, req));
//    }
//
//    @DeleteMapping("/{username}")
//    public ResponseEntity<Void> deleteUser(@PathVariable String username) {
//        userService.deleteUser(username);
//        return ResponseEntity.noContent().build();
//    }
//
//    @PostMapping("/authenticate")
//    public ResponseEntity<String> authenticateUser(@RequestBody UserDTO userDTO) {
//        boolean auth = userService.authenticateUser(userDTO.getUsername(), userDTO.getPassword());
//        if(auth) return ResponseEntity.ok("Authentication successful");
//        else return ResponseEntity.status(401).body("Authentication failed");
//    }
//
//    @GetMapping("/ping")
//    public ResponseEntity<String> ping() {
//        return ResponseEntity.ok("pong");
//    }
//}


package com.example.EVProject.controllers;

import com.example.EVProject.dto.RegistrationRequest;
import com.example.EVProject.dto.UserDTO;
import com.example.EVProject.model.User;
import com.example.EVProject.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
//@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        Optional<User> user = userService.getUserByUsername(username);
        return user.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }


    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody RegistrationRequest userDTO) {
        try {
            User user = userService.createUser(userDTO);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{username}")
    public ResponseEntity<User> updateUser(@PathVariable String username,
                                           @RequestBody RegistrationRequest userDTO) {
        try {
            User user = userService.updateUser(username, userDTO);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Void> deleteUser(@PathVariable String username) {
        try {
            userService.deleteUser(username);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/authenticate")
    public ResponseEntity<String> authenticateUser(@RequestBody UserDTO userDTO) {
        boolean authenticated = userService.authenticateUser(userDTO.getUsername(), userDTO.getPassword());
        if (authenticated) {
            return ResponseEntity.ok("Authentication successful");
        } else {
            return ResponseEntity.status(401).body("Authentication failed");
        }
    }
}
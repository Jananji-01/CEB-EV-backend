package com.example.EVProject.security;

import com.example.EVProject.model.User;
import com.example.EVProject.repositories.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repo;

    public CustomUserDetailsService(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = repo.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Convert User's roles to Spring authorities
        var authorities = u.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName())) // e.g. ROLE_USER
                .toList();

        return new org.springframework.security.core.userdetails.User(
                u.getUsername(),
                u.getPassword(),
                u.getEnabled(),   // enabled
                true,             // accountNonExpired
                true,             // credentialsNonExpired
                true,             // accountNonLocked
                authorities       // user's roles as authorities
        );
    }

}

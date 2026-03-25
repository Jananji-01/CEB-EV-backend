package com.example.EVProject.services;

import com.example.EVProject.dto.RegistrationRequest;
import com.example.EVProject.dto.RooftopSolarOwnerDTO;
import com.example.EVProject.dto.UserDTO;
import com.example.EVProject.model.RooftopSolarOwner;
import com.example.EVProject.model.User;
import com.example.EVProject.repositories.RooftopSolarOwnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RooftopSolarOwnerService {

    @Autowired
    private RooftopSolarOwnerRepository rooftopSolarOwnerRepository;

    @Autowired
    private UserService userService;

    public List<RooftopSolarOwner> getAllRooftopSolarOwners() {
        return rooftopSolarOwnerRepository.findAll();
    }

    public Optional<RooftopSolarOwner> getRooftopSolarOwnerById(Integer solarOwnerId) {
        return rooftopSolarOwnerRepository.findById(solarOwnerId);
    }

    public Optional<RooftopSolarOwner> getRooftopSolarOwnerByUsername(String username) {
        return rooftopSolarOwnerRepository.findByUsername(username);
    }

    public RooftopSolarOwner createRooftopSolarOwner(RooftopSolarOwnerDTO rooftopSolarOwnerDTO) {
        if (rooftopSolarOwnerRepository.existsByUsername(rooftopSolarOwnerDTO.getUsername())) {
            throw new RuntimeException("Rooftop Solar Owner with this username already exists");
        }

        // Create user first
        User user = userService.createUser(convertToUserDTO(rooftopSolarOwnerDTO));

        // Create rooftop solar owner
        RooftopSolarOwner rooftopSolarOwner = new RooftopSolarOwner();
        rooftopSolarOwner.setUsername(rooftopSolarOwnerDTO.getUsername());
        rooftopSolarOwner.setPanelCapacity(rooftopSolarOwnerDTO.getPanelCapacity());
        rooftopSolarOwner.setAddress(rooftopSolarOwnerDTO.getAddress());
        rooftopSolarOwner.setMobileNumber(rooftopSolarOwnerDTO.getMobileNumber());
        rooftopSolarOwner.setEAccountNumber(rooftopSolarOwnerDTO.getEAccountNumber());
        rooftopSolarOwner.setUser(user);

        return rooftopSolarOwnerRepository.save(rooftopSolarOwner);
    }

    public RooftopSolarOwner updateRooftopSolarOwner(Integer solarOwnerId, RooftopSolarOwnerDTO rooftopSolarOwnerDTO) {
        RooftopSolarOwner rooftopSolarOwner = rooftopSolarOwnerRepository.findById(solarOwnerId)
                .orElseThrow(() -> new RuntimeException("Rooftop Solar Owner not found"));

        // Update user information
        userService.updateUser(rooftopSolarOwner.getUsername(), convertToUserDTO(rooftopSolarOwnerDTO));

        // Update rooftop solar owner specific fields
        rooftopSolarOwner.setPanelCapacity(rooftopSolarOwnerDTO.getPanelCapacity());
        rooftopSolarOwner.setAddress(rooftopSolarOwnerDTO.getAddress());
        rooftopSolarOwner.setMobileNumber(rooftopSolarOwnerDTO.getMobileNumber());
        rooftopSolarOwner.setEAccountNumber(rooftopSolarOwnerDTO.getEAccountNumber());

        return rooftopSolarOwnerRepository.save(rooftopSolarOwner);
    }

    public void deleteRooftopSolarOwner(Integer solarOwnerId) {
        RooftopSolarOwner rooftopSolarOwner = rooftopSolarOwnerRepository.findById(solarOwnerId)
                .orElseThrow(() -> new RuntimeException("Rooftop Solar Owner not found"));

        String username = rooftopSolarOwner.getUsername();
        rooftopSolarOwnerRepository.deleteById(solarOwnerId);
        userService.deleteUser(username);
    }

    private RegistrationRequest convertToUserDTO(RooftopSolarOwnerDTO rooftopSolarOwnerDTO) {
        RegistrationRequest userDTO = new RegistrationRequest();
        //com.example.EVProject.dto.UserDTO userDTO = new com.example.EVProject.dto.UserDTO(req.getUsername(), req.getEmail(), req.getPassword());
        userDTO.setUsername(rooftopSolarOwnerDTO.getUsername());
        userDTO.setEmail(rooftopSolarOwnerDTO.getEmail());
        userDTO.setPassword(rooftopSolarOwnerDTO.getPassword());
        return userDTO;

    }
}
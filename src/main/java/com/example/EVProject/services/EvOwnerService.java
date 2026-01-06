package com.example.EVProject.services;

import com.example.EVProject.dto.EvOwnerDTO;
import com.example.EVProject.dto.RegistrationRequest;
import com.example.EVProject.dto.UserDTO;
import com.example.EVProject.model.EvOwner;
import com.example.EVProject.model.User;
import com.example.EVProject.repositories.EvOwnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EvOwnerService {

    @Autowired
    private EvOwnerRepository evOwnerRepository;

    @Autowired
    private UserService userService;

    public List<EvOwner> getAllEvOwners() {
        return evOwnerRepository.findAll();
    }

    public Optional<EvOwner> getEvOwnerById(Integer evOwnerId) {
        return evOwnerRepository.findById(evOwnerId);
    }

    public Optional<EvOwner> getEvOwnerByUsername(String username) {
        return evOwnerRepository.findByUsername(username);
    }

    public EvOwner createEvOwner(EvOwnerDTO evOwnerDTO) {
        if (evOwnerRepository.existsByUsername(evOwnerDTO.getUsername())) {
            throw new RuntimeException("EV Owner with this username already exists");
        }

        // Create user first
        User user = userService.createUser(convertToUserDTO(evOwnerDTO));

        // Create EV owner
        EvOwner evOwner = new EvOwner();
        evOwner.setUsername(evOwnerDTO.getUsername());
        evOwner.setNoOfVehiclesOwned(evOwnerDTO.getNo_of_vehicles_owned());
        evOwner.setMobileNumber(evOwnerDTO.getMobile_number());
        evOwner.setEAccountNumber(evOwnerDTO.getE_account_number());
        evOwner.setUser(user);

        return evOwnerRepository.save(evOwner);
    }

    public EvOwner updateEvOwner(Integer evOwnerId, EvOwnerDTO evOwnerDTO) {
        EvOwner evOwner = evOwnerRepository.findById(evOwnerId)
                .orElseThrow(() -> new RuntimeException("EV Owner not found"));

        // Update user information
        userService.updateUser(evOwner.getUsername(), convertToUserDTO(evOwnerDTO));

        // Update EV owner specific fields
        evOwner.setNoOfVehiclesOwned(evOwnerDTO.getNo_of_vehicles_owned());
        evOwner.setMobileNumber(evOwnerDTO.getMobile_number());
        evOwner.setEAccountNumber(evOwnerDTO.getE_account_number());

        return evOwnerRepository.save(evOwner);
    }

    public void deleteEvOwner(Integer evOwnerId) {
        EvOwner evOwner = evOwnerRepository.findById(evOwnerId)
                .orElseThrow(() -> new RuntimeException("EV Owner not found"));

        String username = evOwner.getUsername();
        evOwnerRepository.deleteById(evOwnerId);
        userService.deleteUser(username);
    }

    private RegistrationRequest convertToUserDTO(EvOwnerDTO evOwnerDTO) {
        RegistrationRequest userDTO = new RegistrationRequest();
        //com.example.EVProject.dto.UserDTO userDTO = new com.example.EVProject.dto.UserDTO(req.getUsername(), req.getEmail(), req.getPassword());
        userDTO.setUsername(evOwnerDTO.getUsername());
        userDTO.setEmail(evOwnerDTO.getEmail());
        userDTO.setPassword(evOwnerDTO.getPassword());
        return userDTO;
    }
}

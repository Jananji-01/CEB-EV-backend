package com.example.EVProject.utils;

import com.example.EVProject.repositories.SmartPlugRepository;
import org.springframework.stereotype.Component;

@Component
public class IdDeviceValidator {

    private final SmartPlugRepository smartPlugRepository;

    public IdDeviceValidator(SmartPlugRepository smartPlugRepository) {
        this.smartPlugRepository = smartPlugRepository;
    }

    /**
     * Validates if the provided IdDevice exists in the smart_plug table.
     * Throws IllegalArgumentException if not found.
     */
    public void validate(String idDevice) {
        boolean exists = smartPlugRepository.existsById(idDevice);
        if (!exists) {
            throw new IllegalArgumentException("Invalid IdDevice: " + idDevice);
        }
    }
}

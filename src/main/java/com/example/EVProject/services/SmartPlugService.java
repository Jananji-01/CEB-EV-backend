//package com.example.EVProject.services;
//
//import com.example.EVProject.dto.SmartPlugDTO;
//import com.example.EVProject.model.SmartPlug;
//import com.example.EVProject.repositories.SmartPlugRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//public class SmartPlugService {
//
//    @Autowired
//    private SmartPlugRepository repository;
//
//    public List<SmartPlugDTO> getAllSmartPlugs() {
//        return repository.findAll().stream().map(this::convertToDto).collect(Collectors.toList());
//    }
//
//    public SmartPlugDTO getSmartPlugById(String id) {
//        SmartPlug plug = repository.findById(id).orElse(null);
//        return plug != null ? convertToDto(plug) : null;
//    }
//
//    public SmartPlugDTO saveSmartPlug(SmartPlugDTO dto) {
//        SmartPlug plug = convertToEntity(dto);
//        SmartPlug saved = repository.save(plug);
//        return convertToDto(saved);
//    }
//
//    public void deleteSmartPlug(String id) {
//        repository.deleteById(id);
//    }
//
//    private SmartPlugDTO convertToDto(SmartPlug plug) {
//        SmartPlugDTO dto = new SmartPlugDTO();
//        dto.setDeviceId(plug.getIdDevice());
//        dto.setCebSerialNo(plug.getCebSerialNo());
//        dto.setMaximumOutput(plug.getMaximumOutput());
//        dto.setStationId(plug.getStationId());
//        return dto;
//    }
//
//    private SmartPlug convertToEntity(SmartPlugDTO dto) {
//        SmartPlug plug = new SmartPlug();
//        plug.setIdDevice(dto.getDeviceId());
//        plug.setCebSerialNo(dto.getCebSerialNo());
//        plug.setMaximumOutput(dto.getMaximumOutput());
//        plug.setStationId(dto.getStationId());
//        return plug;
//    }
//}

package com.example.EVProject.services;

import com.example.EVProject.dto.SmartPlugDTO;
import com.example.EVProject.model.SmartPlug;
import com.example.EVProject.repositories.ChargingStationRepository;
import com.example.EVProject.repositories.SmartPlugRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SmartPlugService {

    @Autowired
    private SmartPlugRepository repository;
    
    @Autowired
    private ChargingStationRepository chargingStationRepository;

    public List<SmartPlugDTO> getAllSmartPlugs() {
        return repository.findAll().stream()
            .map(this::convertToDtoWithDetails)
            .collect(Collectors.toList());
    }

    public SmartPlugDTO getSmartPlugById(String id) {
        SmartPlug plug = repository.findById(id).orElse(null);
        return plug != null ? convertToDtoWithDetails(plug) : null;
    }

    public SmartPlugDTO registerSmartPlug(SmartPlugDTO registrationDTO) {
        // Check if device already exists
        if (repository.existsById(registrationDTO.getIdDevice())) {
            throw new RuntimeException("Smart plug with this ID already exists");
        }

        // Validate station exists if provided
        if (registrationDTO.getStationId() != null) {
            boolean stationExists = chargingStationRepository.existsById(registrationDTO.getStationId());
            if (!stationExists) {
                throw new RuntimeException("Charging station with ID " + registrationDTO.getStationId() + " not found");
            }
        }

        // Create new smart plug
        SmartPlug plug = new SmartPlug();
        plug.setIdDevice(registrationDTO.getIdDevice());
        plug.setCebSerialNo(registrationDTO.getCebSerialNo());
        plug.setMaximumOutput(registrationDTO.getMaximumOutput());
        plug.setStationId(registrationDTO.getStationId());
        plug.setChargePointModel(registrationDTO.getChargePointModel());
        plug.setChargePointVendor(registrationDTO.getChargePointVendor());
        plug.setFirmwareVersion(registrationDTO.getFirmwareVersion());
        
        // Generate QR code data
        String qrData = generateQRCodeData(plug);
        plug.setQrCodeData(qrData);
        plug.setQrCodeGeneratedAt(LocalDateTime.now());

        SmartPlug saved = repository.save(plug);
        SmartPlugDTO responseDTO = convertToDtoWithDetails(saved);
        responseDTO.setRegistrationSuccess(true);
        responseDTO.setMessage("Smart plug registered successfully");
        
        return responseDTO;
    }

    public SmartPlugDTO updateSmartPlug(String id, SmartPlugDTO updateDTO) {
        SmartPlug plug = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Smart plug not found"));

        // Update fields if provided
        if (updateDTO.getCebSerialNo() != null) {
            plug.setCebSerialNo(updateDTO.getCebSerialNo());
        }
        if (updateDTO.getMaximumOutput() != null) {
            plug.setMaximumOutput(updateDTO.getMaximumOutput());
        }
        if (updateDTO.getStationId() != null) {
            plug.setStationId(updateDTO.getStationId());
        }
        if (updateDTO.getChargePointModel() != null) {
            plug.setChargePointModel(updateDTO.getChargePointModel());
        }
        if (updateDTO.getChargePointVendor() != null) {
            plug.setChargePointVendor(updateDTO.getChargePointVendor());
        }
        if (updateDTO.getFirmwareVersion() != null) {
            plug.setFirmwareVersion(updateDTO.getFirmwareVersion());
        }

        SmartPlug updated = repository.save(plug);
        return convertToDtoWithDetails(updated);
    }

    public void deleteSmartPlug(String id) {
        repository.deleteById(id);
    }

    public SmartPlugDTO regenerateQRCode(String idDevice) {
        SmartPlug plug = repository.findById(idDevice)
            .orElseThrow(() -> new RuntimeException("Smart plug not found"));
        
        String newQRData = generateQRCodeData(plug);
        plug.setQrCodeData(newQRData);
        plug.setQrCodeGeneratedAt(LocalDateTime.now());
        
        SmartPlug updated = repository.save(plug);
        SmartPlugDTO responseDTO = convertToDtoWithDetails(updated);
        responseDTO.setMessage("QR code regenerated successfully");
        
        return responseDTO;
    }

    private String generateQRCodeData(SmartPlug plug) {
        // Create a structured JSON for the QR code
        return String.format(
            "{\"deviceId\":\"%s\",\"type\":\"smartplug\",\"model\":\"%s\",\"vendor\":\"%s\",\"maxOutput\":%s,\"timestamp\":\"%s\"}",
            plug.getIdDevice(),
            plug.getChargePointModel() != null ? plug.getChargePointModel() : "Unknown",
            plug.getChargePointVendor() != null ? plug.getChargePointVendor() : "Unknown",
            plug.getMaximumOutput() != null ? plug.getMaximumOutput() : 0,
            LocalDateTime.now().toString()
        );
    }

    private SmartPlugDTO convertToDtoWithDetails(SmartPlug plug) {
        SmartPlugDTO dto = new SmartPlugDTO();
        dto.setIdDevice(plug.getIdDevice());
        dto.setCebSerialNo(plug.getCebSerialNo());
        dto.setMaximumOutput(plug.getMaximumOutput());
        dto.setStationId(plug.getStationId());
        dto.setChargePointModel(plug.getChargePointModel());
        dto.setChargePointVendor(plug.getChargePointVendor());
        dto.setFirmwareVersion(plug.getFirmwareVersion());
        dto.setQrCodeData(plug.getQrCodeData());
        dto.setQrCodeGeneratedAt(plug.getQrCodeGeneratedAt());
        
        // Set additional status information
        if (plug.getQrCodeGeneratedAt() != null) {
            dto.setStatus("REGISTERED");
        } else {
            dto.setStatus("PENDING_REGISTRATION");
        }
        
        return dto;
    }

    private SmartPlug convertToEntity(SmartPlugDTO dto) {

        SmartPlug plug = new SmartPlug();
        plug.setIdDevice(dto.getIdDevice());
        plug.setCebSerialNo(dto.getCebSerialNo());
        plug.setMaximumOutput(dto.getMaximumOutput());
        plug.setStationId(dto.getStationId());
        plug.setChargePointModel(dto.getChargePointModel());
        plug.setChargePointVendor(dto.getChargePointVendor());
        plug.setFirmwareVersion(dto.getFirmwareVersion());
        plug.setQrCodeData(dto.getQrCodeData());
        plug.setQrCodeGeneratedAt(dto.getQrCodeGeneratedAt());
        return plug;
    }
}

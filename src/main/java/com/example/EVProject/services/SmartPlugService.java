// package com.example.EVProject.services;

// import com.example.EVProject.dto.SmartPlugDTO;
// import com.example.EVProject.model.SmartPlug;
// import com.example.EVProject.repositories.ChargingStationRepository;
// import com.example.EVProject.repositories.SmartPlugRepository;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;

// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.stream.Collectors;

// @Service
// public class SmartPlugService {

//     @Autowired
//     private SmartPlugRepository repository;
    
//     @Autowired
//     private ChargingStationRepository chargingStationRepository;

//     public List<SmartPlugDTO> getAllSmartPlugs() {
//         return repository.findAll().stream()
//             .map(this::convertToDtoWithDetails)
//             .collect(Collectors.toList());
//     }

//     public SmartPlugDTO getSmartPlugById(String id) {
//         SmartPlug plug = repository.findById(id).orElse(null);
//         return plug != null ? convertToDtoWithDetails(plug) : null;
//     }

//     public SmartPlugDTO registerSmartPlug(SmartPlugDTO registrationDTO) {
//         // Check if device already exists
//         if (repository.existsById(registrationDTO.getIdDevice())) {
//             throw new RuntimeException("Smart plug with this ID already exists");
//         }

//         // Validate station exists if provided
//         if (registrationDTO.getStationId() != null) {
//             boolean stationExists = chargingStationRepository.existsById(registrationDTO.getStationId());
//             if (!stationExists) {
//                 throw new RuntimeException("Charging station with ID " + registrationDTO.getStationId() + " not found");
//             }
//         }

//         // Create new smart plug
//         SmartPlug plug = new SmartPlug();
//         plug.setIdDevice(registrationDTO.getIdDevice());
//         plug.setCebSerialNo(registrationDTO.getCebSerialNo());
//         plug.setMaximumOutput(registrationDTO.getMaximumOutput());
//         plug.setStationId(registrationDTO.getStationId());
//         plug.setChargePointModel(registrationDTO.getChargePointModel());
//         plug.setChargePointVendor(registrationDTO.getChargePointVendor());
//         plug.setFirmwareVersion(registrationDTO.getFirmwareVersion());
        
//         // Generate QR code data
//         String qrData = generateQRCodeData(plug);
//         plug.setQrCodeData(qrData);
//         plug.setQrCodeGeneratedAt(LocalDateTime.now());

//         SmartPlug saved = repository.save(plug);
//         SmartPlugDTO responseDTO = convertToDtoWithDetails(saved);
//         responseDTO.setRegistrationSuccess(true);
//         responseDTO.setMessage("Smart plug registered successfully");
        
//         return responseDTO;
//     }

//     public SmartPlugDTO updateSmartPlug(String id, SmartPlugDTO updateDTO) {
//         SmartPlug plug = repository.findById(id)
//             .orElseThrow(() -> new RuntimeException("Smart plug not found"));

//         // Update fields if provided
//         if (updateDTO.getCebSerialNo() != null) {
//             plug.setCebSerialNo(updateDTO.getCebSerialNo());
//         }
//         if (updateDTO.getMaximumOutput() != null) {
//             plug.setMaximumOutput(updateDTO.getMaximumOutput());
//         }
//         if (updateDTO.getStationId() != null) {
//             plug.setStationId(updateDTO.getStationId());
//         }
//         if (updateDTO.getChargePointModel() != null) {
//             plug.setChargePointModel(updateDTO.getChargePointModel());
//         }
//         if (updateDTO.getChargePointVendor() != null) {
//             plug.setChargePointVendor(updateDTO.getChargePointVendor());
//         }
//         if (updateDTO.getFirmwareVersion() != null) {
//             plug.setFirmwareVersion(updateDTO.getFirmwareVersion());
//         }

//         SmartPlug updated = repository.save(plug);
//         return convertToDtoWithDetails(updated);
//     }

//     public void deleteSmartPlug(String id) {
//         repository.deleteById(id);
//     }

//     public SmartPlugDTO regenerateQRCode(String idDevice) {
//         SmartPlug plug = repository.findById(idDevice)
//             .orElseThrow(() -> new RuntimeException("Smart plug not found"));
        
//         String newQRData = generateQRCodeData(plug);
//         plug.setQrCodeData(newQRData);
//         plug.setQrCodeGeneratedAt(LocalDateTime.now());
        
//         SmartPlug updated = repository.save(plug);
//         SmartPlugDTO responseDTO = convertToDtoWithDetails(updated);
//         responseDTO.setMessage("QR code regenerated successfully");
        
//         return responseDTO;
//     }

//     private String generateQRCodeData(SmartPlug plug) {
//         // Create a structured JSON for the QR code
//         return String.format(
//             "{\"deviceId\":\"%s\",\"type\":\"smartplug\",\"model\":\"%s\",\"vendor\":\"%s\",\"maxOutput\":%s,\"timestamp\":\"%s\"}",
//             plug.getIdDevice(),
//             plug.getChargePointModel() != null ? plug.getChargePointModel() : "Unknown",
//             plug.getChargePointVendor() != null ? plug.getChargePointVendor() : "Unknown",
//             plug.getMaximumOutput() != null ? plug.getMaximumOutput() : 0,
//             LocalDateTime.now().toString()
//         );
//     }

//     private SmartPlugDTO convertToDtoWithDetails(SmartPlug plug) {
//         SmartPlugDTO dto = new SmartPlugDTO();
//         dto.setIdDevice(plug.getIdDevice());
//         dto.setCebSerialNo(plug.getCebSerialNo());
//         dto.setMaximumOutput(plug.getMaximumOutput());
//         dto.setStationId(plug.getStationId());
//         dto.setChargePointModel(plug.getChargePointModel());
//         dto.setChargePointVendor(plug.getChargePointVendor());
//         dto.setFirmwareVersion(plug.getFirmwareVersion());
//         dto.setQrCodeData(plug.getQrCodeData());
//         dto.setQrCodeGeneratedAt(plug.getQrCodeGeneratedAt());
        
//         // Set additional status information
//         if (plug.getQrCodeGeneratedAt() != null) {
//             dto.setStatus("REGISTERED");
//         } else {
//             dto.setStatus("PENDING_REGISTRATION");
//         }
        
//         return dto;
//     }

//     private SmartPlug convertToEntity(SmartPlugDTO dto) {

//         SmartPlug plug = new SmartPlug();
//         plug.setIdDevice(dto.getIdDevice());
//         plug.setCebSerialNo(dto.getCebSerialNo());
//         plug.setMaximumOutput(dto.getMaximumOutput());
//         plug.setStationId(dto.getStationId());
//         plug.setChargePointModel(dto.getChargePointModel());
//         plug.setChargePointVendor(dto.getChargePointVendor());
//         plug.setFirmwareVersion(dto.getFirmwareVersion());
//         plug.setQrCodeData(dto.getQrCodeData());
//         plug.setQrCodeGeneratedAt(dto.getQrCodeGeneratedAt());
//         return plug;
//     }
// }


package com.example.EVProject.services;

import com.example.EVProject.dto.AssignSmartPlugRequestDTO;
import com.example.EVProject.dto.ChargingStationDTO;
import com.example.EVProject.dto.SmartPlugDTO;
import com.example.EVProject.model.RooftopSolarOwner;
import com.example.EVProject.model.SmartPlug;
import com.example.EVProject.repositories.ChargingStationRepository;
import com.example.EVProject.repositories.RooftopSolarOwnerRepository;
import com.example.EVProject.repositories.SmartPlugRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SmartPlugService {

    @Autowired
    private SmartPlugRepository repository;

    @Autowired
    private ChargingStationService chargingStationService;

    @Autowired
    private RooftopSolarOwnerRepository solarOwnerRepository; 

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
        Optional<RooftopSolarOwner> owner = solarOwnerRepository.findByEAccountNumber(registrationDTO.getAccountNumber());
        if (owner.isEmpty()) {
            throw new RuntimeException("Rooftop solar owner not found with account number: " + registrationDTO.getAccountNumber());
        }

        // Create new smart plug
        SmartPlug plug = new SmartPlug();
        plug.setIdDevice(registrationDTO.getIdDevice());
        plug.setCebSerialNo(registrationDTO.getCebSerialNo());
        plug.setMaximumOutput(registrationDTO.getMaximumOutput());
        plug.setAccountNumber(registrationDTO.getAccountNumber());
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
        responseDTO.setMessage("Smart plug registered successfully. Please assign it to a station.");
        
        return responseDTO;
    }

    @Transactional
    public SmartPlugDTO assignToStation(String idDevice, AssignSmartPlugRequestDTO request) {
        SmartPlug plug = repository.findById(idDevice)
                .orElseThrow(() -> new RuntimeException("Smart plug not found: " + idDevice));

        if ("new".equalsIgnoreCase(request.getAssignmentType())) {
            // Create a new charging station
            ChargingStationDTO newStation = new ChargingStationDTO();
            newStation.setStationName(request.getStationName());
            newStation.setLatitude(request.getLatitude());
            newStation.setLongitude(request.getLongitude());
            newStation.setSolarPowerAvailable(request.getSolarPowerAvailable());
            newStation.setStatus("Available");
            newStation.setErrorCode("NoError");
            newStation.setTimestampCol(LocalDateTime.now());
            newStation.setIdDevice(idDevice);

            // Find the solar owner by account number
            RooftopSolarOwner owner = solarOwnerRepository.findByEAccountNumber(request.getAccountNumber())
                    .orElseThrow(() -> new RuntimeException("Solar owner not found with account: " + request.getAccountNumber()));
            newStation.setSolarOwnerId(owner.getSolarOwnerId());

            ChargingStationDTO savedStation = chargingStationService.saveStation(newStation);

            // Link the smart plug to the new station
            plug.setStationId(savedStation.getStationId());
            repository.save(plug);

        } else if ("existing".equalsIgnoreCase(request.getAssignmentType())) {
            // Assign to an existing station
            if (request.getExistingStationId() == null) {
                throw new RuntimeException("existingStationId is required for assignment type 'existing'");
            }
            // Verify that the station exists
            ChargingStationDTO existingStation = chargingStationService.getStationById(request.getExistingStationId());
            if (existingStation == null) {
                throw new RuntimeException("Station not found: " + request.getExistingStationId());
            }

            plug.setStationId(request.getExistingStationId());
            repository.save(plug);

        } else {
            throw new RuntimeException("Invalid assignment type. Use 'new' or 'existing'.");
        }

        return convertToDtoWithDetails(plug);
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
        dto.setAccountNumber(plug.getAccountNumber());
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
        plug.setChargePointModel(dto.getChargePointModel());
        plug.setChargePointVendor(dto.getChargePointVendor());
        plug.setFirmwareVersion(dto.getFirmwareVersion());
        plug.setQrCodeData(dto.getQrCodeData());
        plug.setQrCodeGeneratedAt(dto.getQrCodeGeneratedAt());
        return plug;
    }
}
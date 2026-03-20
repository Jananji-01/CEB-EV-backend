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

    public List<SmartPlugDTO> getAllSmartPlugs() {
        return repository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public SmartPlugDTO getSmartPlugById(String id) {
        SmartPlug plug = repository.findById(id).orElse(null);
        return plug != null ? convertToDto(plug) : null;
    }

    public SmartPlugDTO saveSmartPlug(SmartPlugDTO dto) {

        SmartPlug plug = convertToEntity(dto);

        // Generate QR Code Data
        String qrData = generateQRCodeData(plug);
        plug.setQrCodeData(qrData);
        plug.setQrCodeGeneratedAt(LocalDateTime.now());

        SmartPlug saved = repository.save(plug);

        SmartPlugDTO response = convertToDto(saved);
        response.setQrCodeData(saved.getQrCodeData());
        response.setQrCodeGeneratedAt(saved.getQrCodeGeneratedAt());

        return response;
    }

    public void deleteSmartPlug(String id) {
        repository.deleteById(id);
    }

    // QR Code data generator
    private String generateQRCodeData(SmartPlug plug) {

        return String.format(
                "{\"deviceId\":\"%s\",\"serial\":\"%s\",\"maxOutput\":%s,\"stationId\":%s,\"timestamp\":\"%s\"}",
                plug.getIdDevice(),
                plug.getCebSerialNo(),
                plug.getMaximumOutput(),
                plug.getStationId(),
                LocalDateTime.now().toString()
        );
    }

    private SmartPlugDTO convertToDto(SmartPlug plug) {

        SmartPlugDTO dto = new SmartPlugDTO();

        dto.setDeviceId(plug.getIdDevice());
        dto.setCebSerialNo(plug.getCebSerialNo());
        dto.setMaximumOutput(plug.getMaximumOutput());
        dto.setStationId(plug.getStationId());

        dto.setQrCodeData(plug.getQrCodeData());
        dto.setQrCodeGeneratedAt(plug.getQrCodeGeneratedAt());

        return dto;
    }

    private SmartPlug convertToEntity(SmartPlugDTO dto) {

        SmartPlug plug = new SmartPlug();

        plug.setIdDevice(dto.getDeviceId());
        plug.setCebSerialNo(dto.getCebSerialNo());
        plug.setMaximumOutput(dto.getMaximumOutput());
        plug.setStationId(dto.getStationId());

        plug.setQrCodeData(dto.getQrCodeData());
        plug.setQrCodeGeneratedAt(dto.getQrCodeGeneratedAt());

        return plug;
    }
    public SmartPlugDTO regenerateQRCode(String id) {

        SmartPlug plug = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Smart plug not found"));

        // Generate new QR
        String newQR = generateQRCodeData(plug);
        plug.setQrCodeData(newQR);
        plug.setQrCodeGeneratedAt(LocalDateTime.now());

        SmartPlug updated = repository.save(plug);

        return convertToDto(updated);
    }
}

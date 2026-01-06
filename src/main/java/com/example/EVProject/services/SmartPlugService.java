package com.example.EVProject.services;

import com.example.EVProject.dto.SmartPlugDTO;
import com.example.EVProject.model.SmartPlug;
import com.example.EVProject.repositories.SmartPlugRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SmartPlugService {

    @Autowired
    private SmartPlugRepository repository;

    public List<SmartPlugDTO> getAllSmartPlugs() {
        return repository.findAll().stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public SmartPlugDTO getSmartPlugById(String id) {
        SmartPlug plug = repository.findById(id).orElse(null);
        return plug != null ? convertToDto(plug) : null;
    }

    public SmartPlugDTO saveSmartPlug(SmartPlugDTO dto) {
        SmartPlug plug = convertToEntity(dto);
        SmartPlug saved = repository.save(plug);
        return convertToDto(saved);
    }

    public void deleteSmartPlug(String id) {
        repository.deleteById(id);
    }

    private SmartPlugDTO convertToDto(SmartPlug plug) {
        SmartPlugDTO dto = new SmartPlugDTO();
        dto.setDeviceId(plug.getIdDevice());
        dto.setCebSerialNo(plug.getCebSerialNo());
        dto.setMaximumOutput(plug.getMaximumOutput());
        dto.setStationId(plug.getStationId());
        return dto;
    }

    private SmartPlug convertToEntity(SmartPlugDTO dto) {
        SmartPlug plug = new SmartPlug();
        plug.setIdDevice(dto.getDeviceId());
        plug.setCebSerialNo(dto.getCebSerialNo());
        plug.setMaximumOutput(dto.getMaximumOutput());
        plug.setStationId(dto.getStationId());
        return plug;
    }
}

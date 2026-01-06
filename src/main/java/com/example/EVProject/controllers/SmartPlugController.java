package com.example.EVProject.controllers;

import com.example.EVProject.dto.SmartPlugDTO;
import com.example.EVProject.services.SmartPlugService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/smart-plugs")
public class SmartPlugController {

    @Autowired
    private SmartPlugService service;

    @GetMapping
    public List<SmartPlugDTO> getAllSmartPlugs() {
        return service.getAllSmartPlugs();
    }

    @GetMapping("/{id}")
    public SmartPlugDTO getSmartPlugById(@PathVariable String id) {
        return service.getSmartPlugById(id);
    }

    @PostMapping
    public SmartPlugDTO saveSmartPlug(@RequestBody SmartPlugDTO dto) {
        return service.saveSmartPlug(dto);
    }

    @DeleteMapping("/{id}")
    public void deleteSmartPlug(@PathVariable String id) {
        service.deleteSmartPlug(id);
    }
}

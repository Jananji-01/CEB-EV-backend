package com.example.EVProject.repositories;

import com.example.EVProject.model.DeviceRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DeviceRegistryRepository extends JpaRepository<DeviceRegistry, Integer> {
    Optional<DeviceRegistry> findByIdDevice(String idDevice);
}

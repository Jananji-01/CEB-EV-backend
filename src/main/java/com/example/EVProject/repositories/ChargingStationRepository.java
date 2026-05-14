//package com.example.EVProject.repositories;
//
//import com.example.EVProject.model.ChargingStation;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//public interface ChargingStationRepository extends JpaRepository<ChargingStation, Integer> {
//
//}

package com.example.EVProject.repositories;

import com.example.EVProject.model.ChargingStation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChargingStationRepository extends JpaRepository<ChargingStation, Integer> {
    Optional<ChargingStation> findByIdDevice(String idDevice);
}


package com.example.EVProject.repositories;

import com.example.EVProject.model.ChargingStation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargingStationRepository extends JpaRepository<ChargingStation, Integer> {

}

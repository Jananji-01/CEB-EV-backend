package com.example.EVProject.repositories;

import com.example.EVProject.model.MonthlyConsumption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonthlyConsumptionRepository extends JpaRepository<MonthlyConsumption, Long> {

    Optional<MonthlyConsumption> findByUsernameAndIdDeviceAndMonthAndYear(
            String username, String idDevice, Integer month, Integer year
    );

    List<MonthlyConsumption> findAllByOrderByYearDescMonthDesc();
}

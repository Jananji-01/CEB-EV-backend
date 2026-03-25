package com.example.EVProject.repositories;

import com.example.EVProject.model.MeterValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeterValueRepository extends JpaRepository<MeterValue, Long> {
    List<MeterValue> findBySession_SessionId(Integer sessionId);
    
    // Custom query to find by device ID through session relationship
    @Query("SELECT mv FROM MeterValue mv WHERE mv.session.idDevice = :deviceId")
    List<MeterValue> findByDeviceId(@Param("deviceId") String deviceId);
}
package com.example.EVProject.repositories;

import com.example.EVProject.model.MeterValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeterValueRepository extends JpaRepository<MeterValue, Long> {
    List<MeterValue> findAllBySession_SessionId(Long sessionId);
}

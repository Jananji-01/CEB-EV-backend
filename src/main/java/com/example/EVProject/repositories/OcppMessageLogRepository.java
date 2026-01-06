package com.example.EVProject.repositories;

import com.example.EVProject.model.OcppMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OcppMessageLogRepository extends JpaRepository<OcppMessageLog, Integer> {
    List<OcppMessageLog> findByIdDevice(String idDevice);
    Optional<OcppMessageLog> findByMessageId(String messageId);
}

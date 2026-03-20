package com.example.EVProject.repositories;

import com.example.EVProject.model.OcppWebSocketSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OcppWebSocketSessionRepository extends JpaRepository<OcppWebSocketSession, Integer> {
    Optional<OcppWebSocketSession> findBySessionId(String sessionId);
    Optional<OcppWebSocketSession> findByIdDevice(String idDevice);
}
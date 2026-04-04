package com.example.EVProject.services;

import com.example.EVProject.dto.ChargingSessionDTO;
import com.example.EVProject.dto.ChargingStationDTO;
import com.example.EVProject.dto.SolarOwnerConsumptionDTO;
import com.example.EVProject.model.ChargingSession;
import com.example.EVProject.repositories.ChargingSessionRepository;
import com.example.EVProject.repositories.ChargingStationRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChargingSessionService {

    @Autowired
    private ChargingSessionRepository repository;

    @Autowired
    private ChargingStationRepository repo;

    public List<ChargingSessionDTO> getAllSessions() {
        // make sure you call stream() properly
        return repository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public ChargingSessionDTO getSessionById(Integer id) {
        // make sure orElse is called on Optional
        return repository.findById(id)
                .map(this::convertToDto)
                .orElse(null);
    }

//    public ChargingStationDTO getStationById(Integer id){
//        return repo.findById(id)
//                .map(this::convertToDto)
//                .orElse(null);
//    }

    public ChargingSessionDTO saveSession(ChargingSessionDTO dto) {
        ChargingSession session = convertToEntity(dto);
        ChargingSession saved = repository.save(session);
        return convertToDto(saved);
    }

    public void deleteSession(Integer id) {
        repository.deleteById(id);
    }

    public List<ChargingSessionDTO> getSessionsBySolarOwnerId(Integer ownerId) {
        return repository.findBySolarOwnerId(ownerId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }


    private ChargingSessionDTO convertToDto(ChargingSession session) {
        ChargingSessionDTO dto = new ChargingSessionDTO();
        dto.setSessionId(session.getSessionId());
        dto.setStartTime(session.getStartTime());
        dto.setEndTime(session.getEndTime());
        dto.setSoc(session.getSoc());
        dto.setChargingMode(session.getChargingMode());
        dto.setTotalConsumption(session.getTotalConsumption());
        dto.setAmount(session.getAmount());
        dto.setIdDevice(session.getIdDevice());
        dto.setEvOwnerAccountNo(session.getEvOwnerAccountNo());
        dto.setMeterStart(session.getMeterStart());
        dto.setStatus(session.getStatus()); 
        
        // Optional: Set status based on endTime
        if (session.getEndTime() == null) {
            dto.setStatus("ACTIVE");
        } else {
            dto.setStatus("COMPLETED");
        }
        return dto;
    }

    private ChargingSession convertToEntity(ChargingSessionDTO dto) {
        ChargingSession session = new ChargingSession();
        session.setSessionId(dto.getSessionId());
        session.setStartTime(dto.getStartTime());
        session.setEndTime(dto.getEndTime());
        session.setSoc(dto.getSoc());
        session.setChargingMode(dto.getChargingMode());
        session.setTotalConsumption(dto.getTotalConsumption());
        session.setAmount(dto.getAmount());
        session.setIdDevice(dto.getIdDevice());
        session.setEvOwnerAccountNo(dto.getEvOwnerAccountNo());
        session.setMeterStart(dto.getMeterStart());
        session.setStatus(dto.getStatus());
        return session;
    }

    public Double getTotalConsumptionBySolarOwner(Integer ownerId) {
        return repository.getTotalConsumptionBySolarOwner(ownerId);
    }

    public SolarOwnerConsumptionDTO getMonthlyConsumptionByOwner(Integer ownerId) {
        return repository.getMonthlyConsumptionByOwner(ownerId);
    }


    public List<ChargingSessionDTO> getSessionsForCurrentMonth() {
        return repository.findSessionsForCurrentMonth()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Integer startNewChargingSession(String idDevice, String idTag, Integer connectorId, Long meterStart, String evOwnerAccountNo) {
        // 1️⃣ Check for existing active session
        Optional<ChargingSession> activeSession = repository.findByIdDeviceAndEndTimeIsNull(idDevice);
        if (activeSession.isPresent()) {
            throw new IllegalStateException("ConcurrentTx");
        }

        // 2️⃣ Create a new session
        ChargingSession session = new ChargingSession();
        session.setIdDevice(idDevice);
        session.setStartTime(LocalDateTime.now());
        session.setChargingMode("NORMAL");
        session.setTotalConsumption(0.0);
        session.setAmount(0.0);
        session.setSoc(0.0);
        session.setEvOwnerAccountNo(evOwnerAccountNo);
        session.setMeterStart(meterStart);  // ← Store the starting meter value
        
        System.out.println("Session created with meterStart: " + meterStart);

        // 3️⃣ Save the new session
        ChargingSession savedSession = repository.save(session);

        // 4️⃣ Return its session ID
        return savedSession.getSessionId();
    }

    // Add to ChargingSessionService.java
    public ChargingSession getActiveSession(String idDevice) {
        return repository.findByIdDeviceAndEndTimeIsNull(idDevice)
                .orElse(null);
    }

    public List<ChargingSession> getActiveSessions() {
        return repository.findAll().stream()
                .filter(session -> session.getEndTime() == null)
                .collect(Collectors.toList());
    }

    @Transactional
    public void endChargingSession(Integer transactionId, Long meterStop, String timestampStr) {
        System.out.println("=== endChargingSession ===");
        System.out.println("Transaction ID: " + transactionId);
        System.out.println("Meter Stop: " + meterStop);
        System.out.println("Timestamp from request (ignored): " + timestampStr);
        
        var sessionOpt = repository.findById(transactionId);
        if (sessionOpt.isEmpty()) {
            System.out.println("❌ Session not found for transactionId: " + transactionId);
            throw new IllegalArgumentException("Session not found for transactionId: " + transactionId);
        }

        ChargingSession session = sessionOpt.get();
        System.out.println("Found session: " + session.getSessionId());
        System.out.println("Meter Start: " + session.getMeterStart());
        
        // ✅ ALWAYS use current time when stop button is clicked
        LocalDateTime endTime = LocalDateTime.now();
        session.setEndTime(endTime);
        System.out.println("Using current time: " + endTime);
        
        // ✅ Calculate total consumption correctly
        if (meterStop != null) {
            if (session.getMeterStart() != null) {
                // Calculate actual consumption by subtracting meterStart from meterStop
                double consumption = (double) (meterStop - session.getMeterStart());
                session.setTotalConsumption(consumption);
                
                // Calculate amount based on consumption (example: $0.15 per kWh)
                double amount = consumption * 0.15;
                session.setAmount(amount);
                
                System.out.println("Meter Start: " + session.getMeterStart());
                System.out.println("Meter Stop: " + meterStop);
                System.out.println("Total consumption: " + consumption + " kWh");
                System.out.println("Amount: $" + String.format("%.2f", amount));
            } else {
                // Fallback if meterStart is not available in the database
                session.setTotalConsumption(meterStop.doubleValue());
                double amount = meterStop.doubleValue() * 0.15;
                session.setAmount(amount);
                System.out.println("⚠️ MeterStart not available, using meterStop as consumption: " + meterStop + " kWh");
            }
        } else {
            System.out.println("⚠️ MeterStop is null, consumption not calculated");
        }
        
        session.setStatus("COMPLETED");
        
        // Save the updated session
        ChargingSession savedSession = repository.save(session);
        System.out.println("✅ Session saved with end time: " + savedSession.getEndTime());
        System.out.println("✅ Total consumption: " + savedSession.getTotalConsumption());
        System.out.println("✅ Amount: $" + String.format("%.2f", savedSession.getAmount()));
        System.out.println("✅ Status: " + savedSession.getStatus());
        System.out.println("=== endChargingSession completed ===");
    }
}
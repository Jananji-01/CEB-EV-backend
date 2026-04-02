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
            // There’s already an active charging session — reject (ConcurrentTx)
            throw new IllegalStateException("ConcurrentTx");
        }

        // 2️⃣ Create a new session
        ChargingSession session = new ChargingSession();
        session.setIdDevice(idDevice);
        session.setStartTime(LocalDateTime.now());
        session.setChargingMode("NORMAL");    // default
        session.setTotalConsumption(0.0);
        session.setAmount(0.0);
        session.setSoc(0.0);
        session.setEvOwnerAccountNo(evOwnerAccountNo);
        session.setMeterStart(meterStart);
        session.setStatus("ACTIVE");

        System.out.println("Session created - ID: " + session.getSessionId() + 
                       ", MeterStart: " + meterStart + 
                       ", Status: ACTIVE");


        // 3️⃣ Save the new session
        ChargingSession savedSession = repository.save(session);

        System.out.println("✅ Session created with ID: " + savedSession.getSessionId());

        // 4️⃣ Return its session ID (used as transactionId)
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

        var sessionOpt = repository.findById(transactionId);
        if (sessionOpt.isEmpty()) {

        System.out.println("=== endChargingSession ===");
        System.out.println("Transaction ID: " + transactionId);
        System.out.println("Meter Stop: " + meterStop);
        System.out.println("Timestamp: " + timestampStr);
        
        if (sessionOpt.isEmpty()) {
            System.out.println("❌ Session not found for transactionId: " + transactionId);
            throw new IllegalArgumentException("Session not found for transactionId: " + transactionId);
        }

        ChargingSession session = sessionOpt.get();
        session.setEndTime(LocalDateTime.parse(timestampStr.replace("Z", "")));
        session.setTotalConsumption((double) (meterStop != null ? meterStop : 0));
        repository.save(session);
        System.out.println("Found session: " + session.getSessionId());
        System.out.println("Current end time: " + session.getEndTime());
        System.out.println("Current Status: " + session.getStatus());
        
        // Parse timestamp or use current time if not provided
        LocalDateTime endTime;
        if (timestampStr != null && !timestampStr.isEmpty()) {
            endTime = LocalDateTime.parse(timestampStr.replace("Z", ""));
            System.out.println("Using provided timestamp: " + endTime);
        } else {
            endTime = LocalDateTime.now();
            System.out.println("Using current time: " + endTime);
        }
        
        session.setEndTime(endTime);
        
        // ✅ Calculate total consumption correctly
        if (meterStop != null && session.getMeterStart() != null) {
            double consumption = (double) (meterStop - session.getMeterStart());
            session.setTotalConsumption(consumption);
            
            // Calculate amount based on consumption (example: $0.15 per kWh)
            double amount = consumption * 0.15;
            session.setAmount(amount);
            
            System.out.println("Meter Start: " + session.getMeterStart());
            System.out.println("Meter Stop: " + meterStop);
            System.out.println("Total consumption: " + consumption + " kWh");
            System.out.println("Amount: $" + String.format("%.2f", amount));
        } else if (meterStop != null) {
            // Fallback if meterStart is not available
            session.setTotalConsumption(meterStop.doubleValue());
            double amount = meterStop.doubleValue() * 0.15;
            session.setAmount(amount);
            System.out.println("⚠️ MeterStart not available, using meterStop as consumption");
        }
        
        // ✅ Set status to COMPLETED
        session.setStatus("COMPLETED");
        
        
        // Save the updated session
        ChargingSession savedSession = repository.save(session);
        System.out.println("✅ Session saved:");
        System.out.println("   - End Time: " + savedSession.getEndTime());
        System.out.println("   - Total Consumption: " + savedSession.getTotalConsumption() + " kWh");
        System.out.println("   - Amount: $" + String.format("%.2f", savedSession.getAmount()));
        System.out.println("   - Status: " + savedSession.getStatus());
    }

}
}

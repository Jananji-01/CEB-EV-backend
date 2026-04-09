package com.example.EVProject.services;

import com.example.EVProject.dto.ChargingSessionDTO;
import com.example.EVProject.dto.ChargingStationDTO;
import com.example.EVProject.dto.SolarOwnerConsumptionDTO;
import com.example.EVProject.model.ChargingSession;
import com.example.EVProject.model.MonthlyConsumption;
import com.example.EVProject.model.SmartPlug;
import com.example.EVProject.repositories.ChargingSessionRepository;
import com.example.EVProject.repositories.ChargingStationRepository;
import com.example.EVProject.repositories.MonthlyConsumptionRepository;
import com.example.EVProject.repositories.SmartPlugRepository;
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

    @Autowired
    private MonthlyConsumptionRepository monthlyConsumptionRepository;

    @Autowired
    private SmartPlugRepository smartPlugRepository;

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
        dto.setEAccountNo(session.getEAccountNo());
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
        session.setEAccountNo(dto.getEAccountNo());
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

    public Integer startNewChargingSession(String idDevice, String idTag, Integer connectorId, Long meterStart, String eAccountNo) {
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
        session.setEAccountNo(eAccountNo);
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
        System.out.println("Current end_time in DB before update: " + session.getEndTime());
        System.out.println("Meter Start: " + session.getMeterStart());
        
        // ✅ ALWAYS use current time when stop button is clicked
        LocalDateTime endTime = LocalDateTime.now();
        System.out.println("Setting end_time to CURRENT TIME: " + endTime);
        session.setEndTime(endTime);

         double consumption = 0.0;
        
        // ✅ Calculate total consumption correctly
        if (meterStop != null) {
            if (session.getMeterStart() != null) {
                // Calculate actual consumption by subtracting meterStart from meterStop
                consumption = (double) (meterStop - session.getMeterStart());
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
        System.out.println("✅ AFTER SAVE - End time in saved entity: " + savedSession.getEndTime());
        System.out.println("✅ Total consumption: " + savedSession.getTotalConsumption());
        System.out.println("✅ Amount: $" + String.format("%.2f", savedSession.getAmount()));
        System.out.println("✅ Status: " + savedSession.getStatus());

        // ✅ UPDATE MONTHLY CONSUMPTION TABLE
        updateMonthlyConsumption(session, consumption, endTime);
        
        // Verify by fetching fresh from database
        var verifySession = repository.findById(transactionId);
        if (verifySession.isPresent()) {
            System.out.println("✅ VERIFICATION FROM DB - End time: " + verifySession.get().getEndTime());
        }
        
        System.out.println("=== endChargingSession completed ===");
    }

        private void updateMonthlyConsumption(ChargingSession session, double consumption, LocalDateTime endTime) {
        try {
            // Get month and year from the end time
            int month = endTime.getMonthValue();
            int year = endTime.getYear();
            
            // Use getUsernameFromIdDevice instead
            String username = getUsernameFromIdDevice(session.getIdDevice());
            if (username == null || username.isEmpty()) {
                username = session.getIdDevice(); // Use idDevice as fallback
            }
            
            // Get eAccountNumber - provide fallback
            String eAccountNumber = session.getEAccountNo();
            if (eAccountNumber == null || eAccountNumber.isEmpty()) {
                eAccountNumber = "UNKNOWN_" + session.getIdDevice(); // Generate a fallback value
            }
            
            System.out.println("=== Updating Monthly Consumption ===");
            System.out.println("Username: " + username);
            System.out.println("IdDevice: " + session.getIdDevice());
            System.out.println("Month: " + month + ", Year: " + year);
            System.out.println("Consumption to add: " + consumption + " kWh");
            
            // Calculate duration in minutes (if start time is available)
            int durationMinutes = 0;
            if (session.getStartTime() != null && endTime != null) {
                durationMinutes = (int) java.time.Duration.between(session.getStartTime(), endTime).toMinutes();
                System.out.println("Duration minutes: " + durationMinutes);
            }
            
            // Find existing monthly consumption record
            Optional<MonthlyConsumption> existingOpt = monthlyConsumptionRepository
                    .findByUsernameAndIdDeviceAndMonthAndYear(
                            username, 
                            session.getIdDevice(), 
                            month, 
                            year
                    );
            
            if (existingOpt.isPresent()) {
                // Update existing record
                MonthlyConsumption mc = existingOpt.get();
                double newTotalConsumption = mc.getTotalConsumption() + consumption;
                int newTotalSessions = mc.getTotalSessions() + 1;
                int newTotalDuration = mc.getTotalDurationMinutes() + durationMinutes;
                
                mc.setTotalConsumption(newTotalConsumption);
                mc.setTotalSessions(newTotalSessions);
                mc.setTotalDurationMinutes(newTotalDuration);
                mc.setCreatedAt(LocalDateTime.now());
                
                monthlyConsumptionRepository.save(mc);
                System.out.println("✅ Updated existing monthly consumption record");
                System.out.println("   New total consumption: " + newTotalConsumption + " kWh");
                System.out.println("   Total sessions: " + newTotalSessions);
                System.out.println("   Total duration: " + newTotalDuration + " minutes");
            } else {
                // Create new record
                MonthlyConsumption mc = new MonthlyConsumption();
                mc.setUsername(username);
                mc.setIdDevice(session.getIdDevice());
                mc.setMonth(month);
                mc.setYear(year);
                mc.setTotalConsumption(consumption);
                mc.setTotalSessions(1);
                mc.setTotalDurationMinutes(durationMinutes);
                mc.setCreatedAt(LocalDateTime.now());
                mc.setEAccountNumber(session.getEAccountNo());
                
                monthlyConsumptionRepository.save(mc);
                System.out.println("✅ Created new monthly consumption record");
                System.out.println("   Total consumption: " + consumption + " kWh");
                System.out.println("   Sessions: 1");
                System.out.println("   Duration: " + durationMinutes + " minutes");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error updating monthly consumption: " + e.getMessage());
            e.printStackTrace();
        }
    }

        private String getUsernameFromIdDevice(String idDevice) {
            try {
                // Fetch username from SmartPlug table
                Optional<SmartPlug> plugOpt = smartPlugRepository.findById(idDevice);
                if (plugOpt.isPresent() && plugOpt.get().getUsername() != null) {
                    return plugOpt.get().getUsername();
                }
            } catch (Exception e) {
                System.err.println("Error fetching username for idDevice: " + idDevice);
            }
            // Return idDevice as fallback if username not found
            return idDevice;
        }
}
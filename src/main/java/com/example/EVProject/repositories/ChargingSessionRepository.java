package com.example.EVProject.repositories;

import com.example.EVProject.dto.SolarOwnerConsumptionDTO;
import com.example.EVProject.model.ChargingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChargingSessionRepository extends JpaRepository<ChargingSession, Integer> {
    Optional<ChargingSession> findByIdDeviceAndEndTimeIsNull(String idDevice);

    @Query("select s from ChargingSession s " +
            "join s.smartPlug sp " +
            "join sp.chargingStation cs " +
            "where cs.solarOwnerId = :ownerId")
    List<ChargingSession> findBySolarOwnerId(@Param("ownerId") Integer ownerId);

    @Query("select sum(s.totalConsumption) from ChargingSession s " +
            "join s.smartPlug sp " +
            "join sp.chargingStation cs " +
            "where cs.solarOwnerId = :ownerId")
    Double getTotalConsumptionBySolarOwner(@Param("ownerId") Integer ownerId);

    @Query("select new com.example.EVProject.dto.SolarOwnerConsumptionDTO(o.username, sum(s.totalConsumption)) " +
            "from ChargingSession s " +
            "join s.smartPlug sp " +
            "join sp.chargingStation cs " +
            "join cs.solarOwner o " +
            "where o.solarOwnerId = :ownerId " +
            "and extract(month from s.startTime) = extract(month from current_date) " +
            "and extract(year from s.startTime) = extract(year from current_date) " +
            "group by o.username")
    SolarOwnerConsumptionDTO getMonthlyConsumptionByOwner(@Param("ownerId") Integer ownerId);

    @Query("select s from ChargingSession s " +
            "where extract(month from s.startTime) = extract(month from current_date) " +
            "and extract(year from s.startTime) = extract(year from current_date)")
    List<ChargingSession> findSessionsForCurrentMonth();

    @Query(value = """
        SELECT COUNT(*)
        FROM charging_sessions cs
        WHERE cs.id_device = :deviceId
          AND EXTRACT(MONTH FROM cs.start_time) = :month
          AND EXTRACT(YEAR  FROM cs.start_time) = :year
        """, nativeQuery = true)
    Integer countMonthlySessions(@Param("deviceId") String deviceId,
                                 @Param("month") int month,
                                 @Param("year") int year);

    @Query(value = """
        SELECT COALESCE(SUM(cs.total_consumption), 0)
        FROM charging_sessions cs
        WHERE cs.id_device = :deviceId
          AND EXTRACT(MONTH FROM cs.start_time) = :month
          AND EXTRACT(YEAR  FROM cs.start_time) = :year
        """, nativeQuery = true)
    Double sumMonthlyConsumption(@Param("deviceId") String deviceId,
                                 @Param("month") int month,
                                 @Param("year") int year);

    @Query(value = """
        SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (cs.end_time - cs.start_time)) / 60), 0)
        FROM charging_sessions cs
        WHERE cs.id_device = :deviceId
          AND cs.end_time IS NOT NULL
          AND cs.start_time IS NOT NULL
          AND EXTRACT(MONTH FROM cs.start_time) = :month
          AND EXTRACT(YEAR  FROM cs.start_time) = :year
        """, nativeQuery = true)
    Double sumMonthlyDurationMinutes(@Param("deviceId") String deviceId,
                                     @Param("month") int month,
                                     @Param("year") int year);



}

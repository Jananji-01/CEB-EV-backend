package com.example.EVProject.repositories;

import com.example.EVProject.model.OcppMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OcppMessageLogRepository extends JpaRepository<OcppMessageLog, Long> {

    // Basic find methods
    List<OcppMessageLog> findByIdDevice(String idDevice);

    Optional<OcppMessageLog> findByMessageId(String messageId);

    /**
     * Find MeterValues messages for a specific device within a time range
     * @param action the OCPP action (e.g., "MeterValues")
     * @param startTime start of time range
     * @param endTime end of time range
     * @return list of OCPP messages
     */
    List<OcppMessageLog> findByActionAndReceivedAtBetween(@Param("action") String action,
                                                          @Param("startTime") LocalDateTime startTime,
                                                          @Param("endTime") LocalDateTime endTime);

    /**
     * Find MeterValues messages for a specific device
     * @param deviceId the device ID
     * @param action the OCPP action
     * @param startTime start of time range
     * @param endTime end of time range
     * @return list of OCPP messages
     */
    List<OcppMessageLog> findByIdDeviceAndActionAndReceivedAtBetween(@Param("deviceId") String deviceId,
                                                                     @Param("action") String action,
                                                                     @Param("startTime") LocalDateTime startTime,
                                                                     @Param("endTime") LocalDateTime endTime);

    /**
     * Find all MeterValues messages for a device ordered by received time
     * @param deviceId the device ID
     * @param action the OCPP action
     * @return list of OCPP messages ordered by received time
     */
    @Query("SELECT m FROM OcppMessageLog m WHERE m.idDevice = :deviceId AND m.action = :action ORDER BY m.receivedAt ASC")
    List<OcppMessageLog> findMeterValuesByDevice(@Param("deviceId") String deviceId,
                                                 @Param("action") String action);

    /**
     * Find MeterValues messages within a time range for a specific session
     * @param deviceId the device ID
     * @param action the OCPP action
     * @param sessionStart session start time
     * @param sessionEnd session end time
     * @return list of OCPP messages
     */
    @Query("SELECT m FROM OcppMessageLog m WHERE " +
            "m.idDevice = :deviceId AND " +
            "m.action = :action AND " +
            "m.receivedAt BETWEEN :sessionStart AND :sessionEnd " +
            "ORDER BY m.receivedAt ASC")
    List<OcppMessageLog> findMeterValuesForSession(@Param("deviceId") String deviceId,
                                                   @Param("action") String action,
                                                   @Param("sessionStart") LocalDateTime sessionStart,
                                                   @Param("sessionEnd") LocalDateTime sessionEnd);

    List<OcppMessageLog> findByIdDeviceAndActionAndReceivedAtBetweenOrderByReceivedAtAsc(
            String idDevice, String action, LocalDateTime start, LocalDateTime end);

    @Query("SELECT o FROM OcppMessageLog o WHERE o.idDevice = :idDevice " +
            "AND o.action = 'MeterValues' " +
            "AND o.receivedAt BETWEEN :start AND :end " +
            "ORDER BY o.receivedAt ASC")
    List<OcppMessageLog> findMeterValuesBetween(
            @Param("idDevice") String idDevice,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(o) FROM OcppMessageLog o WHERE o.idDevice = :idDevice " +
            "AND o.action = 'MeterValues' " +
            "AND o.receivedAt BETWEEN :start AND :end")
    Integer countMeterValuesBetween(
            @Param("idDevice") String idDevice,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query(value = "SELECT * FROM OCPP_MESSAGE_LOG o WHERE o.ID_DEVICE = :idDevice " +
            "AND o.ACTION = 'MeterValues' " +
            "AND o.RECEIVED_AT BETWEEN :start AND :end " +
            "ORDER BY o.RECEIVED_AT ASC", nativeQuery = true)
    List<OcppMessageLog> findMeterValuesBetweenNative(
            @Param("idDevice") String idDevice,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT MIN(o.receivedAt) FROM OcppMessageLog o WHERE o.idDevice = :idDevice AND o.action = 'MeterValues'")
    LocalDateTime findFirstMeterValueTime(@Param("idDevice") String idDevice);

    @Query("SELECT MAX(o.receivedAt) FROM OcppMessageLog o WHERE o.idDevice = :idDevice AND o.action = 'MeterValues'")
    LocalDateTime findLastMeterValueTime(@Param("idDevice") String idDevice);
}
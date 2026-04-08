package com.example.EVProject.repositories;

import com.example.EVProject.model.IdTagInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface IdTagInfoRepository extends JpaRepository<IdTagInfo, Integer> {
    Optional<IdTagInfo> findByIdDevice(String idDevice);
    Optional<IdTagInfo> findByIdTag(String idTag);
    Optional<IdTagInfo> findByIdTagAndIdDevice(String idTag, String idDevice);

    @Query(value = """
        SELECT * FROM (
            SELECT * FROM id_tag_info 
            WHERE id_device = :idDevice 
            ORDER BY created_at DESC
        ) WHERE ROWNUM = 1
        """, nativeQuery = true)
    Optional<IdTagInfo> findTopByIdDeviceOrderByCreatedAtDesc(@Param("idDevice") String idDevice);
}
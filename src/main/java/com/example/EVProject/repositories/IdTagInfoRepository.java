package com.example.EVProject.repositories;

import com.example.EVProject.model.IdTagInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface IdTagInfoRepository extends JpaRepository<IdTagInfo, Integer> {
    Optional<IdTagInfo> findByIdDevice(String idDevice);
    Optional<IdTagInfo> findByIdTag(String idTag);
    Optional<IdTagInfo> findByIdTagAndIdDevice(String idTag, String idDevice);
}

// package com.example.EVProject.repositories;

// import com.example.EVProject.model.IdTagInfo;
// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.query.Param;
// import java.util.Optional;
// import java.util.List;

// public interface IdTagInfoRepository extends JpaRepository<IdTagInfo, Integer> {
//     List<IdTagInfo> findByIdDevice(String idDevice);
//     Optional<IdTagInfo> findByIdTag(String idTag);
//     Optional<IdTagInfo> findByIdTagAndIdDevice(String idTag, String idDevice);
// }

//     @Query(value = """
//         SELECT * FROM (
//             SELECT * FROM id_tag_info 
//             WHERE id_device = :idDevice 
//             ORDER BY created_at DESC
//         ) WHERE ROWNUM = 1
//         """, nativeQuery = true)
//     Optional<IdTagInfo> findTopByIdDeviceOrderByCreatedAtDesc(@Param("idDevice") String idDevice);
// }


package com.example.EVProject.repositories;

import com.example.EVProject.model.IdTagInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface IdTagInfoRepository extends JpaRepository<IdTagInfo, Integer> {

    List<IdTagInfo> findByIdDevice(String idDevice);

    Optional<IdTagInfo> findByIdTag(String idTag);

        // ✅ Returns List (for when you want all records)
    List<IdTagInfo> findByIdTagAndIdDevice(String idTag, String idDevice);
    
    // ✅ Returns Optional (for when you expect single result)
    Optional<IdTagInfo> findFirstByIdTagAndIdDeviceOrderByCreatedAtDesc(String idTag, String idDevice);
    

    @Query(value = "SELECT * FROM (SELECT * FROM DACONS16.id_tag_info WHERE id_device = ?1 ORDER BY created_at DESC) WHERE ROWNUM = 1", nativeQuery = true)
    Optional<IdTagInfo> findTopByIdDeviceOrderByCreatedAtDesc(String idDevice);

    // Custom query MUST be inside the interface
    @Query(value = """
        SELECT * FROM (
            SELECT * FROM id_tag_info 
            WHERE id_device = :idDevice 
            ORDER BY created_at DESC
        ) WHERE ROWNUM = 1
        """, nativeQuery = true)
    Optional<IdTagInfo> findLatestByIdDevice(@Param("idDevice") String idDevice);
}
//package com.example.EVProject.repositories;
//
//import com.example.EVProject.model.SmartPlug;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.Optional;
//
//public interface SmartPlugRepository extends JpaRepository<SmartPlug, String> {
//    Optional<SmartPlug> findFirstByCebSerialNo(String cebSerialNo);
//
//}


package com.example.EVProject.repositories;

import com.example.EVProject.model.SmartPlug;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface SmartPlugRepository extends JpaRepository<SmartPlug, String> {


    @Query(value = """
        SELECT * FROM (
            SELECT * FROM smart_plug 
            WHERE ceb_serial_no = :cebSerialNo 
            ORDER BY id_device
        ) WHERE ROWNUM = 1
        """, nativeQuery = true)
    Optional<SmartPlug> findFirstByCebSerialNo(@Param("cebSerialNo") String cebSerialNo);
}

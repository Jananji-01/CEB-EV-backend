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

import java.util.Optional;

public interface SmartPlugRepository extends JpaRepository<SmartPlug, String> {

    Optional<SmartPlug> findFirstByCebSerialNo(String cebSerialNo);

}

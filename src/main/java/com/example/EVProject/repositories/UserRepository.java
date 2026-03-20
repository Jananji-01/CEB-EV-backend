//package com.example.EVProject.repositories;
//
//import com.example.EVProject.model.User;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//import java.util.Optional;
//
//@Repository
//public interface UserRepository extends JpaRepository<User, String> {
//    Optional<User> findByEmail(String email);
//    boolean existsByEmail(String email);
//    boolean existsByUsername(String username);
//}

package com.example.EVProject.repositories;

import com.example.EVProject.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    // Keep the original method for findByEmail if needed elsewhere
    @Query(value = "SELECT * FROM dacons16.app_user WHERE email = :email AND ROWNUM = 1", nativeQuery = true)
    Optional<User> findByEmail(@Param("email") String email);

    // Native query for checking email existence
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM dacons16.app_user WHERE email = :email", nativeQuery = true)
    int existsByEmailNative(@Param("email") String email);

    // Native query for checking username existence
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM dacons16.app_user WHERE username = :username", nativeQuery = true)
    int existsByUsernameNative(@Param("username") String username);

    // Keep the standard JPA method if needed elsewhere
    boolean existsById(String username);

    boolean existsByUsername(String username);
}

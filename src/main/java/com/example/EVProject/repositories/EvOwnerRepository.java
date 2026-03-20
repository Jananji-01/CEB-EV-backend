package com.example.EVProject.repositories;

import com.example.EVProject.model.EvOwner;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvOwnerRepository extends JpaRepository<EvOwner, Integer> {
    Optional<EvOwner> findByUsername(String username);

    boolean existsByUsername(String username);
//
//    @Query(value = """
//        SELECT
//          eo.e_account_number AS accountNo,
//          eo.username AS username,
//          u.email AS email,
//          eo.mobile_number AS contactNo,
//          eo.no_of_vehicles_owned AS noOfVehiclesOwned
//        FROM ev_owner eo
//        JOIN "user" u ON u.username = eo.username
//        WHERE (:accountNo IS NULL OR LOWER(eo.e_account_number) LIKE LOWER(CONCAT('%', :accountNo, '%')))
//          AND (:username IS NULL OR LOWER(eo.username) LIKE LOWER(CONCAT('%', :username, '%')))
//        ORDER BY eo.e_account_number
//        """, nativeQuery = true)
//    List<Object[]> findAdminEvOwnersRaw(
//            @Param("accountNo") String accountNo,
//            @Param("username") String username
//    );
//}


    // Explicit query to avoid naming resolution issues
    @Query("SELECT e FROM EvOwner e WHERE e.eAccountNumber = :eAccountNumber")
    Optional<EvOwner> findByEAccountNumber(@Param("eAccountNumber") String eAccountNumber);

    // Also add explicit query for idTag if needed (but derived should work; kept for consistency)
    @Query("SELECT e FROM EvOwner e WHERE e.idTag = :idTag")
    Optional<EvOwner> findByIdTag(@Param("idTag") String idTag);

    @Query(value = """

            SELECT
                                  eo.E_ACCOUNT_NUMBER,
                                  eo.USERNAME,
                                  u.EMAIL,
                                  eo.MOBILE_NUMBER,
                                  eo.NO_OF_VEHICLES_OWNED
                                FROM EV_OWNER eo
                                JOIN APP_USER u ON u.USERNAME = eo.USERNAME
                                WHERE (:accountNo IS NULL OR LOWER(eo.E_ACCOUNT_NUMBER) LIKE LOWER('%' || :accountNo || '%'))
                                  AND (:username IS NULL OR LOWER(eo.USERNAME) LIKE LOWER('%' || :username || '%'))
                                ORDER BY eo.E_ACCOUNT_NUMBER
    """, nativeQuery = true)
List<Object[]> findAdminEvOwnersRaw(
        @Param("accountNo") String accountNo,
        @Param("username") String username
);
}
package com.example.EVProject.repositories;

import com.example.EVProject.model.RooftopSolarOwner;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RooftopSolarOwnerRepository extends JpaRepository<RooftopSolarOwner, Integer> {
    Optional<RooftopSolarOwner> findByUsername(String username);
    boolean existsByUsername(String username);

    @Query(value = """
    SELECT
      r.e_account_number AS accountNo,
      r.username AS username,
      COALESCE(SUM(mc.total_consumption), 0) AS totalKwh,
      u.email AS email,
      r.mobile_number AS contactNo
    FROM rooftop_solar_owner r
    JOIN users u ON u.username = r.username
    LEFT JOIN monthly_consumption mc
           ON mc.e_account_number = r.e_account_number
          AND mc.month = :month
          AND mc.year  = :year
    WHERE (:accountNo IS NULL OR LOWER(r.e_account_number) LIKE LOWER('%' || :accountNo || '%'))
      AND (:username IS NULL OR LOWER(r.username) LIKE LOWER('%' || :username || '%'))
    GROUP BY r.e_account_number, r.username, u.email, r.mobile_number
    ORDER BY r.e_account_number
    """, nativeQuery = true)
    List<Object[]> getSolarOwnerRowsRaw(
            @Param("month") int month,
            @Param("year") int year,
            @Param("accountNo") String accountNo,
            @Param("username") String username
    );

}
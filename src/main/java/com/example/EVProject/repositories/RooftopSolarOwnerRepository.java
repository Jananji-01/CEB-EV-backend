package com.example.EVProject.repositories;

import com.example.EVProject.model.RooftopSolarOwner;
import io.lettuce.core.dynamic.annotation.Param;
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
      r.E_ACCOUNT_NUMBER,
      r.USERNAME,
      COALESCE(SUM(mc.TOTAL_CONSUMPTION), 0),
      u.EMAIL,
      r.MOBILE_NUMBER
    FROM ROOFTOP_SOLAR_OWNER r
    JOIN APP_USER u ON u.USERNAME = r.USERNAME
    LEFT JOIN MONTHLY_CONSUMPTION mc
           ON mc.E_ACCOUNT_NUMBER = r.E_ACCOUNT_NUMBER
          AND mc.MONTH = :month
          AND mc.YEAR  = :year
    WHERE (:accountNo IS NULL OR LOWER(r.E_ACCOUNT_NUMBER) LIKE LOWER('%' || :accountNo || '%'))
      AND (:username IS NULL OR LOWER(r.USERNAME) LIKE LOWER('%' || :username || '%'))
    GROUP BY r.E_ACCOUNT_NUMBER, r.USERNAME, u.EMAIL, r.MOBILE_NUMBER
    ORDER BY r.E_ACCOUNT_NUMBER
""", nativeQuery = true)
    List<Object[]> getSolarOwnerRowsRaw(
            @Param("month") int month,
            @Param("year") int year,
            @Param("accountNo") String accountNo,
            @Param("username") String username
    );
}
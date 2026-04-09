package com.example.EVProject.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@Table(
        name = "monthly_consumption",
        uniqueConstraints = @UniqueConstraint(columnNames = {"username", "id_device", "month", "year"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "monthly_consumption_seq")
    @SequenceGenerator(name = "monthly_consumption_seq", sequenceName = "MONTHLY_CONSUMPTION_SEQ", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "e_account_number", nullable = false)
    private String eAccountNumber;

    @Column(name = "id_device", nullable = false)
    private String idDevice;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "total_sessions")
    private Integer totalSessions;

    @Column(name = "total_consumption")
    private Double totalConsumption;

    @Column(name = "total_duration_minutes")
    private Integer totalDurationMinutes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

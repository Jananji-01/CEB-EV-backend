package com.example.EVProject.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "meter_values")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long meterId;

    @Column(nullable = false)
    private Integer connectorId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChargingSession session;

    @OneToMany(mappedBy = "meterValue", cascade = CascadeType.ALL, orphanRemoval = true,fetch = FetchType.EAGER)
    private List<SampledValue> sampledValues;
}

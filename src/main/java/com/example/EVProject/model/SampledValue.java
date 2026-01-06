package com.example.EVProject.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sampled_values")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampledValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sampleId;

    private Double value;
    private String context;
    private String format;
    private String measurand;
    private String location;
    private String unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meter_id", nullable = false)
    private MeterValue meterValue;
}

//package com.example.EVProject.model;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//@Entity
//@Table(name = "sampled_values")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class SampledValue {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long sampleId;
//
//    private Double value;
//    private String context;
//    private String format;
//    private String measurand;
//    private String location;
//    private String unit;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "meter_id", nullable = false)
//    private MeterValue meterValue;
//}


package com.example.EVProject.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SAMPLED_VALUES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampledValue {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sampled_values_seq")
    @SequenceGenerator(
            name = "sampled_values_seq",
            sequenceName = "SAMPLED_VALUES_SEQ",
            allocationSize = 1
    )
    @Column(name = "SAMPLE_ID")
    private Long sampleId;

    @Column(name = "VALUE")
    private Double value;

    private String context;
    private String format;
    private String measurand;
    private String location;
    private String unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "METER_ID", nullable = false)
    private MeterValue meterValue;
}
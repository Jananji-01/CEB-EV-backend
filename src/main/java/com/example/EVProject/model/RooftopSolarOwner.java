package com.example.EVProject.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "rooftop_solar_owner")
public class RooftopSolarOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "solar_owner_id")
    private Integer solarOwnerId;

    @Column(name = "username", unique = true)
    private String username;

    @Column(name = "panel_capacity")
    private Double panelCapacity;

    @Column(name = "address", columnDefinition = "text")
    private String address;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "e_account_number")
    private String eAccountNumber;

    @OneToOne
    @JoinColumn(name = "username", referencedColumnName = "username", insertable = false, updatable = false)
    private User user;

    // getters and setters

}

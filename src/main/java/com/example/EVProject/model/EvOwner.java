package com.example.EVProject.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "EV_OWNER")
public class EvOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ev_owner_seq")
    @SequenceGenerator(name = "ev_owner_seq", sequenceName = "EV_OWNER_SEQ", allocationSize = 1)
    @Column(name = "EV_OWNER_ID")
    private Long evOwnerId;

    @Column(name = "USERNAME", unique = true,nullable = false)
    private String username;

    @Column(name = "NO_OF_VEHICLES_OWNED")
    private Integer noOfVehiclesOwned;

    @Column(name = "MOBILE_NUMBER")
    private String mobileNumber;

    @Column(name = "E_ACCOUNT_NUMBER")
    private String eAccountNumber;

    @Column(name = "ID_TAG")
    private String idTag;

    @OneToOne
    @JoinColumn(name = "USERNAME", referencedColumnName = "USERNAME",insertable = false,updatable = false)
    private User user;

    // getters and setters

//    public Integer getEvOwnerId() {
//        return evOwnerId;
//    }
//
//    public void setEvOwnerId(Integer evOwnerId) {
//        this.evOwnerId = evOwnerId;
//    }
//
//    public String getUsername() {
//        return username;
//    }
//
//    public void setUsername(String username) {
//        this.username = username;
//    }
//
//    public Integer getNoOfVehiclesOwned() {
//        return noOfVehiclesOwned;
//    }
//
//    public void setNoOfVehiclesOwned(Integer noOfVehiclesOwned) {
//        this.noOfVehiclesOwned = noOfVehiclesOwned;
//    }
//
//    public String getMobileNumber() {
//        return mobileNumber;
//    }
//
//    public void setMobileNumber(String mobileNumber) {
//        this.mobileNumber = mobileNumber;
//    }
//
//    public String getEAccountNumber() {
//        return eAccountNumber;
//    }
//
//    public void setEAccountNumber(String eAccountNumber) {
//        this.eAccountNumber = eAccountNumber;
//    }
//
//    public User getUser() {
//        return user;
//    }
//
//    public void setUser(User user) {
//        this.user = user;
//    }
}

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
@Table(name = "ev_owner")
public class EvOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ev_owner_id")
    private Integer evOwnerId;

    @Column(name = "username", unique = true,nullable = false)
    private String username;

    @Column(name = "no_of_vehicles_owned")
    private Integer noOfVehiclesOwned;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "e_account_number")
    private String eAccountNumber;

    @OneToOne
    @JoinColumn(name = "username", referencedColumnName = "username",insertable = false,updatable = false)
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

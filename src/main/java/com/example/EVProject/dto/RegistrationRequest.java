package com.example.EVProject.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "E-account number is required")
    @Size(min = 10, message = "E-account number must be at least 10 characters")
    private String e_account_number;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Role is required")
    private String role; // USER, ADMIN, EVOWNER, ROOFTOPSOLAROWNER

    // EV_OWNER fields
    private String mobile_number;
    private Integer no_of_vehicles_owned; // Only for EV_OWNER

    // ROOFTOP_SOLAR_OWNER fields
    private String address;
    private Double solarCapacity; // Only for ROOFTOPSOLAROWNER
}
//    // Getters & Setters
//    public String getUsername() { return username; }
//    public void setUsername(String username) { this.username = username; }
//
//    public String getEmail() { return email; }
//    public void setEmail(String email) { this.email = email; }
//
//    public String getEaccount_no() { return eaccount_no; }
//    public void setEaccount_no(String eaccount_no) { this.eaccount_no = eaccount_no; }
//
//    public String getPassword() { return password; }
//    public void setPassword(String password) { this.password = password; }
//
//    public String getRole() { return role; }
//    public void setRole(String role) { this.role = role; }
//
//    public String getMobileNumber() { return mobileNumber; }
//    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
//
//    public Integer getNoOfVehiclesOwned() {
//        return noOfVehiclesOwned;
//    }
//    public void setNoOfVehiclesOwned(Integer noOfVehiclesOwned) {
//        this.noOfVehiclesOwned = noOfVehiclesOwned;
//    }
//
//    public String getAddress() { return address; }
//    public void setAddress(String address) { this.address = address; }
//
//    public Double getSolarCapacity() { return solarCapacity; }
//    public void setSolarCapacity(Double solarCapacity) { this.solarCapacity = solarCapacity; }
//}

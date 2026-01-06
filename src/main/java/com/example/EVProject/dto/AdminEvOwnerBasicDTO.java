package com.example.EVProject.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminEvOwnerBasicDTO {
    private String accountNo;         // ev_owner.e_account_number
    private String username;          // ev_owner.username
    private String email;             // user.email
    private String contactNo;         // ev_owner.mobile_number
    private Integer noOfVehiclesOwned;// ev_owner.no_of_vehicles_owned
}

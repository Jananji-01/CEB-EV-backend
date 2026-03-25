package com.example.EVProject.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminSolarOwnerRowDTO {
    private String accountNo;
    private String username;
    private Double totalKwh;   // monthly consumption
    private String email;
    private String contactNo;
}

package com.example.EVProject.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class RooftopSolarOwnerDTO {
    private Integer solarOwnerId;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @DecimalMin(value = "0.1", message = "Panel capacity must be greater than 0")
    private Double panelCapacity;

    @NotBlank(message = "Address is required")
    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String address;

    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobileNumber;

    @NotBlank(message = "Account number is required")
    private String eAccountNumber;
}
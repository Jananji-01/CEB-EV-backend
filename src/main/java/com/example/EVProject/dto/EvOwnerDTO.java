package com.example.EVProject.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class EvOwnerDTO {
    private Integer evOwnerId;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Min(value = 1, message = "Number of vehicles must be at least 1")
    private Integer no_of_vehicles_owned;

    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobile_number;

    @NotBlank(message = "Account number is required")
    private String e_account_number;
}

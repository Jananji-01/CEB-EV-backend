// package com.example.EVProject.dto;

// import lombok.Data;
// import lombok.AllArgsConstructor;
// import lombok.NoArgsConstructor;

// @Data
// @NoArgsConstructor  
// @AllArgsConstructor 
// public class MonthlyConsumptionRequest {
//     private String username;
//     private String eAccountNumber;

//     // optional if you want select month later
//     private Integer month;
//     private Integer year;
    
// }

package com.example.EVProject.dto;

public class MonthlyConsumptionRequest {
    private String username;
    private String eAccountNumber;
    private Integer month;
    private Integer year;
    
    // No-args constructor
    public MonthlyConsumptionRequest() {}
    
    // Getters
    public String getUsername() { return username; }
    public String getEAccountNumber() { return eAccountNumber; }
    public Integer getMonth() { return month; }
    public Integer getYear() { return year; }
    
    // Setters
    public void setUsername(String username) { this.username = username; }
    public void setEAccountNumber(String eAccountNumber) { this.eAccountNumber = eAccountNumber; }
    public void setMonth(Integer month) { this.month = month; }
    public void setYear(Integer year) { this.year = year; }
}
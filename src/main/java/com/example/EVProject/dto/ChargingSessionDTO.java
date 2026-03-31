<<<<<<< HEAD
package com.example.EVProject.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

=======
// package com.example.EVProject.dto;

// import lombok.Getter;
// import lombok.Setter;

// import java.time.LocalDateTime;

// @Setter
// @Getter
// public class ChargingSessionDTO {

//     private Integer sessionId;
//     private LocalDateTime startTime;
//     private LocalDateTime endTime;
//     private Double soc;
//     private String chargingMode;
//     private Double totalConsumption;
//     private Double amount;
//     private String idDevice;




//     // getters and setters

// }


package com.example.EVProject.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

>>>>>>> d22e5da8fc6a82b034607c878e2b6dd632f0e2b0
@Setter
@Getter
public class ChargingSessionDTO {

    private Integer sessionId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double soc;
    private String chargingMode;
    private Double totalConsumption;
    private Double amount;
    private String idDevice;
<<<<<<< HEAD




    // getters and setters

}
=======
    private String evOwnerAccountNo; 
    private Long meterStart;
    
    // Optional: Add status field if needed for frontend
    private String status;  // This can be derived from endTime being null or not
    
    public boolean isActive() {
        return "ACTIVE".equals(status) || (endTime == null && status == null);
    }
    
    // Helper method to get duration in minutes
    public Long getDurationMinutes() {
        if (startTime == null) return 0L;
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).toMinutes();
    }

    public Double getCalculatedConsumption(Long meterStop) {
        if (meterStart != null && meterStop != null) {
            return (double) (meterStop - meterStart);
        }
        return totalConsumption;
    }
}
>>>>>>> d22e5da8fc6a82b034607c878e2b6dd632f0e2b0

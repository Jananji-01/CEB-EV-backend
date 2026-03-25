package com.example.EVProject.model;

import jakarta.persistence.*;

@Entity
@Table(name = "ev")
public class Ev {

    @Id
    @Column(name = "vehicle_number")
    private String vehicleNumber;

    @Column(name = "model")
    private String model;

    @Column(name = "battery_capacity")
    private Double batteryCapacity;

    @Column(name = "maximum_charging_input")
    private Double maximumChargingInput;

    @Column(name = "ev_owner_id")
    private Integer evOwnerId;

    @ManyToOne
    @JoinColumn(name = "ev_owner_id", referencedColumnName = "ev_owner_id", insertable = false, updatable = false)
    private EvOwner evOwner;

    // getters and setters

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getBatteryCapacity() {
        return batteryCapacity;
    }

    public void setBatteryCapacity(Double batteryCapacity) {
        this.batteryCapacity = batteryCapacity;
    }

    public Double getMaximumChargingInput() {
        return maximumChargingInput;
    }

    public void setMaximumChargingInput(Double maximumChargingInput) {
        this.maximumChargingInput = maximumChargingInput;
    }

    public Integer getEvOwnerId() {
        return evOwnerId;
    }

    public void setEvOwnerId(Integer evOwnerId) {
        this.evOwnerId = evOwnerId;
    }

    public EvOwner getEvOwner() {
        return evOwner;
    }

    public void setEvOwner(EvOwner evOwner) {
        this.evOwner = evOwner;
    }
}


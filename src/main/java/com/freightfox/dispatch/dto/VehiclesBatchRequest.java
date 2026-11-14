package com.freightfox.dispatch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class VehiclesBatchRequest {

    @NotEmpty(message = "vehicles list must not be empty")
    @Valid
    private List<VehicleRequestItem> vehicles;

    public VehiclesBatchRequest() {
    }

    public List<VehicleRequestItem> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<VehicleRequestItem> vehicles) {
        this.vehicles = vehicles;
    }
}
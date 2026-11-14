package com.freightfox.dispatch.dto;

import java.util.List;

public class VehicleDispatchPlanDto {

    private String vehicleId;
    private double totalLoad;
    private String totalDistance;
    private List<AssignedOrderDto> assignedOrders;

    public VehicleDispatchPlanDto() {
    }

    public VehicleDispatchPlanDto(String vehicleId, double totalLoad, String totalDistance,
                                  List<AssignedOrderDto> assignedOrders) {
        this.vehicleId = vehicleId;
        this.totalLoad = totalLoad;
        this.totalDistance = totalDistance;
        this.assignedOrders = assignedOrders;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public double getTotalLoad() {
        return totalLoad;
    }

    public void setTotalLoad(double totalLoad) {
        this.totalLoad = totalLoad;
    }

    public String getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(String totalDistance) {
        this.totalDistance = totalDistance;
    }

    public List<AssignedOrderDto> getAssignedOrders() {
        return assignedOrders;
    }

    public void setAssignedOrders(List<AssignedOrderDto> assignedOrders) {
        this.assignedOrders = assignedOrders;
    }
}
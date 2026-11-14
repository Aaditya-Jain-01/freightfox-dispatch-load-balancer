package com.freightfox.dispatch.dto;

import java.util.List;

public class DispatchPlanResponse {

    private List<VehicleDispatchPlanDto> dispatchPlan;
    private List<AssignedOrderDto> unassignedOrders;

    public DispatchPlanResponse() {
    }

    public DispatchPlanResponse(List<VehicleDispatchPlanDto> dispatchPlan,
                                List<AssignedOrderDto> unassignedOrders) {
        this.dispatchPlan = dispatchPlan;
        this.unassignedOrders = unassignedOrders;
    }

    public List<VehicleDispatchPlanDto> getDispatchPlan() {
        return dispatchPlan;
    }

    public void setDispatchPlan(List<VehicleDispatchPlanDto> dispatchPlan) {
        this.dispatchPlan = dispatchPlan;
    }

    public List<AssignedOrderDto> getUnassignedOrders() {
        return unassignedOrders;
    }

    public void setUnassignedOrders(List<AssignedOrderDto> unassignedOrders) {
        this.unassignedOrders = unassignedOrders;
    }
}
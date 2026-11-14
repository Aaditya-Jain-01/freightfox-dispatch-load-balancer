package com.freightfox.dispatch.dto;

import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Priority;

public class AssignedOrderDto {

    private String orderId;
    private double latitude;
    private double longitude;
    private String address;
    private double packageWeight;
    private Priority priority;

    public AssignedOrderDto() {
    }

    public AssignedOrderDto(String orderId, double latitude, double longitude, String address,
                            double packageWeight, Priority priority) {
        this.orderId = orderId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.packageWeight = packageWeight;
        this.priority = priority;
    }

    public static AssignedOrderDto fromEntity(DeliveryOrder order) {
        return new AssignedOrderDto(
                order.getOrderId(),
                order.getLatitude(),
                order.getLongitude(),
                order.getAddress(),
                order.getPackageWeight(),
                order.getPriority()
        );
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getPackageWeight() {
        return packageWeight;
    }

    public void setPackageWeight(double packageWeight) {
        this.packageWeight = packageWeight;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }
}
package com.freightfox.dispatch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class OrdersBatchRequest {

    @NotEmpty(message = "orders list must not be empty")
    @Valid
    private List<OrderRequestItem> orders;

    public OrdersBatchRequest() {
    }

    public List<OrderRequestItem> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderRequestItem> orders) {
        this.orders = orders;
    }
}
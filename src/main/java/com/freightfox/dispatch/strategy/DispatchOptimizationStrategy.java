package com.freightfox.dispatch.strategy;

import com.freightfox.dispatch.dto.DispatchPlanResponse;
import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Vehicle;
import java.util.List;

public interface DispatchOptimizationStrategy {
    DispatchPlanResponse optimize(List<Vehicle> vehicles, List<DeliveryOrder> orders);
}
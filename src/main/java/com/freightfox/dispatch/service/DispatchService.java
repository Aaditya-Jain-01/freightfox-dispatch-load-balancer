package com.freightfox.dispatch.service;

import com.freightfox.dispatch.dto.DispatchPlanResponse;
import com.freightfox.dispatch.dto.OrderRequestItem;
import com.freightfox.dispatch.dto.VehicleRequestItem;
import com.freightfox.dispatch.exception.InvalidDispatchStateException;
import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Vehicle;
import com.freightfox.dispatch.repository.DeliveryOrderRepository;
import com.freightfox.dispatch.repository.VehicleRepository;
import com.freightfox.dispatch.strategy.DispatchOptimizationStrategy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DispatchService {

    private final DeliveryOrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final DispatchOptimizationStrategy optimizationStrategy;

    public DispatchService(DeliveryOrderRepository orderRepository,
                           VehicleRepository vehicleRepository,
                           DispatchOptimizationStrategy optimizationStrategy) {
        this.orderRepository = orderRepository;
        this.vehicleRepository = vehicleRepository;
        this.optimizationStrategy = optimizationStrategy;
    }

    @Transactional
    public void saveOrders(List<OrderRequestItem> items) {
        Map<String, DeliveryOrder> deduplicated = new LinkedHashMap<>();
        for (OrderRequestItem item : items) {
            DeliveryOrder entity = new DeliveryOrder(
                    item.getOrderId().trim(),
                    item.getLatitude(),
                    item.getLongitude(),
                    item.getAddress().trim(),
                    item.getPackageWeight(),
                    item.getPriority()
            );
            deduplicated.put(entity.getOrderId(), entity);
        }
        orderRepository.saveAll(new ArrayList<>(deduplicated.values()));
    }

    @Transactional
    public void saveVehicles(List<VehicleRequestItem> items) {
        Map<String, Vehicle> deduplicated = new LinkedHashMap<>();
        for (VehicleRequestItem item : items) {
            Vehicle entity = new Vehicle(
                    item.getVehicleId().trim(),
                    item.getCapacity(),
                    item.getCurrentLatitude(),
                    item.getCurrentLongitude(),
                    item.getCurrentAddress().trim()
            );
            deduplicated.put(entity.getVehicleId(), entity);
        }
        vehicleRepository.saveAll(new ArrayList<>(deduplicated.values()));
    }

    @Transactional(readOnly = true)
    public DispatchPlanResponse generateDispatchPlan() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        if (vehicles.isEmpty()) {
            throw new InvalidDispatchStateException("No vehicles found in fleet. Please add vehicles first.");
        }

        List<DeliveryOrder> orders = orderRepository.findAll();
        if (orders.isEmpty()) {
            throw new InvalidDispatchStateException("No delivery orders found. Please add orders first.");
        }

        return optimizationStrategy.optimize(vehicles, orders);
    }

    @Transactional
    public void clearAllData() {
        orderRepository.deleteAllInBatch();
        vehicleRepository.deleteAllInBatch();
    }
}
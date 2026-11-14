package com.freightfox.dispatch.strategy;

import com.freightfox.dispatch.dto.AssignedOrderDto;
import com.freightfox.dispatch.dto.DispatchPlanResponse;
import com.freightfox.dispatch.dto.VehicleDispatchPlanDto;
import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Vehicle;
import com.freightfox.dispatch.util.HaversineCalculator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PriorityNearestNeighborStrategy implements DispatchOptimizationStrategy {

    @Override
    public DispatchPlanResponse optimize(List<Vehicle> vehicles, List<DeliveryOrder> orders) {
        List<Vehicle> sortedVehicles = new ArrayList<>(vehicles);
        sortedVehicles.sort(Comparator.comparing(Vehicle::getVehicleId));

        Map<String, VehicleRouteState> stateMap = new LinkedHashMap<>();
        for (Vehicle vehicle : sortedVehicles) {
            stateMap.put(vehicle.getVehicleId(), new VehicleRouteState(vehicle));
        }

        // Sort orders strictly by Priority rank (HIGH -> MEDIUM -> LOW), then heavier first for better bin-packing, then orderId
        List<DeliveryOrder> prioritizedOrders = new ArrayList<>(orders);
        prioritizedOrders.sort(
                Comparator.comparingInt((DeliveryOrder o) -> o.getPriority().getRank())
                        .thenComparing(Comparator.comparingDouble(DeliveryOrder::getPackageWeight).reversed())
                        .thenComparing(DeliveryOrder::getOrderId)
        );

        List<AssignedOrderDto> unassignedOrders = new ArrayList<>();

        for (DeliveryOrder order : prioritizedOrders) {
            VehicleRouteState bestCandidate = null;
            double minAddedDistance = Double.MAX_VALUE;

            for (VehicleRouteState candidate : stateMap.values()) {
                if (candidate.remainingCapacity() + 1e-9 < order.getPackageWeight()) {
                    continue;
                }

                double distanceFromLastPoint = HaversineCalculator.calculateDistanceKm(
                        candidate.currentLat,
                        candidate.currentLon,
                        order.getLatitude(),
                        order.getLongitude()
                );

                // Pick closest feasible vehicle; break ties by higher remaining capacity
                if (distanceFromLastPoint < minAddedDistance - 1e-6
                        || (Math.abs(distanceFromLastPoint - minAddedDistance) <= 1e-6
                        && bestCandidate != null
                        && candidate.remainingCapacity() > bestCandidate.remainingCapacity())) {
                    minAddedDistance = distanceFromLastPoint;
                    bestCandidate = candidate;
                }
            }

            if (bestCandidate == null) {
                unassignedOrders.add(AssignedOrderDto.fromEntity(order));
            } else {
                bestCandidate.assign(order);
            }
        }

        List<VehicleDispatchPlanDto> planList = new ArrayList<>();
        for (VehicleRouteState state : stateMap.values()) {
            List<DeliveryOrder> optimizedRoute = optimizeStopSequence(state.vehicle, state.assigned);
            double totalDistanceKm = computeTotalRouteDistance(state.vehicle, optimizedRoute);

            List<AssignedOrderDto> assignedDtos = optimizedRoute.stream()
                    .map(AssignedOrderDto::fromEntity)
                    .toList();

            planList.add(new VehicleDispatchPlanDto(
                    state.vehicle.getVehicleId(),
                    roundTwoDecimals(state.currentLoad),
                    formatDistance(totalDistanceKm),
                    assignedDtos
            ));
        }

        return new DispatchPlanResponse(planList, unassignedOrders);
    }

    /**
     * Re-sequences a vehicle's assigned stops via greedy nearest-neighbor starting from the vehicle's depot/current coordinates.
     */
    private List<DeliveryOrder> optimizeStopSequence(Vehicle vehicle, List<DeliveryOrder> assigned) {
        if (assigned.size() <= 1) {
            return new ArrayList<>(assigned);
        }

        List<DeliveryOrder> remaining = new ArrayList<>(assigned);
        List<DeliveryOrder> ordered = new ArrayList<>(assigned.size());

        double curLat = vehicle.getCurrentLatitude();
        double curLon = vehicle.getCurrentLongitude();

        while (!remaining.isEmpty()) {
            int nearestIdx = 0;
            double nearestDist = Double.MAX_VALUE;

            for (int i = 0; i < remaining.size(); i++) {
                DeliveryOrder candidate = remaining.get(i);
                double dist = HaversineCalculator.calculateDistanceKm(
                        curLat, curLon, candidate.getLatitude(), candidate.getLongitude()
                );
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearestIdx = i;
                }
            }

            DeliveryOrder next = remaining.remove(nearestIdx);
            ordered.add(next);
            curLat = next.getLatitude();
            curLon = next.getLongitude();
        }

        return ordered;
    }

    private double computeTotalRouteDistance(Vehicle vehicle, List<DeliveryOrder> orderedStops) {
        double totalKm = 0.0;
        double curLat = vehicle.getCurrentLatitude();
        double curLon = vehicle.getCurrentLongitude();

        for (DeliveryOrder stop : orderedStops) {
            totalKm += HaversineCalculator.calculateDistanceKm(
                    curLat, curLon, stop.getLatitude(), stop.getLongitude()
            );
            curLat = stop.getLatitude();
            curLon = stop.getLongitude();
        }
        return totalKm;
    }

    private String formatDistance(double distanceKm) {
        double rounded = roundTwoDecimals(distanceKm);
        if (rounded == Math.rint(rounded)) {
            return String.format(Locale.US, "%.0f km", rounded);
        }
        return String.format(Locale.US, "%.2f km", rounded);
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static final class VehicleRouteState {
        private final Vehicle vehicle;
        private final List<DeliveryOrder> assigned = new ArrayList<>();
        private double currentLoad = 0.0;
        private double currentLat;
        private double currentLon;

        private VehicleRouteState(Vehicle vehicle) {
            this.vehicle = vehicle;
            this.currentLat = vehicle.getCurrentLatitude();
            this.currentLon = vehicle.getCurrentLongitude();
        }

        private double remainingCapacity() {
            return vehicle.getCapacity() - currentLoad;
        }

        private void assign(DeliveryOrder order) {
            assigned.add(order);
            currentLoad += order.getPackageWeight();
            currentLat = order.getLatitude();
            currentLon = order.getLongitude();
        }
    }
}
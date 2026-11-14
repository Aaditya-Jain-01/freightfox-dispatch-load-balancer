package com.freightfox.dispatch.controller;

import com.freightfox.dispatch.dto.ApiStatusResponse;
import com.freightfox.dispatch.dto.DispatchPlanResponse;
import com.freightfox.dispatch.dto.OrdersBatchRequest;
import com.freightfox.dispatch.dto.VehiclesBatchRequest;
import com.freightfox.dispatch.service.DispatchService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Dispatch Load Balancer", description = "Fleet dispatch optimization endpoints")
public class DispatchController {

    private final DispatchService dispatchService;

    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @Hidden
    @GetMapping("/")
    public void redirectToSwagger(HttpServletResponse response) throws IOException {
        response.sendRedirect("/swagger-ui.html");
    }

    @GetMapping("/health")
    @Operation(summary = "Health check endpoint")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @PostMapping("/api/dispatch/orders")
    @Operation(summary = "Submit or update delivery orders")
    public ResponseEntity<ApiStatusResponse> submitOrders(@Valid @RequestBody OrdersBatchRequest request) {
        dispatchService.saveOrders(request.getOrders());
        return ResponseEntity.ok(new ApiStatusResponse("success", "Delivery orders accepted."));
    }

    @PostMapping("/api/dispatch/vehicles")
    @Operation(summary = "Submit or update fleet vehicles")
    public ResponseEntity<ApiStatusResponse> submitVehicles(@Valid @RequestBody VehiclesBatchRequest request) {
        dispatchService.saveVehicles(request.getVehicles());
        return ResponseEntity.ok(new ApiStatusResponse("success", "Vehicle details accepted."));
    }

    @GetMapping("/api/dispatch/plan")
    @Operation(summary = "Compute optimized dispatch plan across active fleet")
    public ResponseEntity<DispatchPlanResponse> getDispatchPlan() {
        return ResponseEntity.ok(dispatchService.generateDispatchPlan());
    }

    @DeleteMapping("/api/dispatch/reset")
    @Operation(summary = "Clear all stored orders and vehicles")
    public ResponseEntity<ApiStatusResponse> resetData() {
        dispatchService.clearAllData();
        return ResponseEntity.ok(new ApiStatusResponse("success", "All orders and vehicles cleared."));
    }
}
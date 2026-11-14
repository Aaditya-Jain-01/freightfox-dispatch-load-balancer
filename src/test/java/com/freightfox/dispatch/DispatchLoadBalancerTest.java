package com.freightfox.dispatch;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freightfox.dispatch.service.DispatchService;
import com.freightfox.dispatch.util.HaversineCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class DispatchLoadBalancerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void resetDatabase() {
        dispatchService.clearAllData();
    }

    @Test
    void shouldCalculateAccurateHaversineDistances() {
        assertEquals(0.0, HaversineCalculator.calculateDistanceKm(28.6139, 77.2090, 28.6139, 77.2090), 1e-6);

        // Indiranagar (12.9716, 77.6413) to MG Road (12.9716, 77.5946) ~ 5.06 km
        double bangaloreDist = HaversineCalculator.calculateDistanceKm(12.9716, 77.6413, 12.9716, 77.5946);
        assertEquals(5.06, bangaloreDist, 0.1);
    }

    @Test
    void shouldAssignHighPriorityOrdersBeforeMediumAndLowWithinCapacity() throws Exception {
        String vehiclesJson = """
                {
                  "vehicles": [
                    {
                      "vehicleId": "VEH001",
                      "capacity": 30,
                      "currentLatitude": 28.6139,
                      "currentLongitude": 77.2090,
                      "currentAddress": "Connaught Place, Delhi, India"
                    }
                  ]
                }
                """;

        String ordersJson = """
                {
                  "orders": [
                    {
                      "orderId": "ORD_LOW",
                      "latitude": 28.6139,
                      "longitude": 77.2090,
                      "address": "Connaught Place, Delhi, India",
                      "packageWeight": 25,
                      "priority": "LOW"
                    },
                    {
                      "orderId": "ORD_HIGH",
                      "latitude": 28.7041,
                      "longitude": 77.1025,
                      "address": "Karol Bagh, Delhi, India",
                      "packageWeight": 25,
                      "priority": "HIGH"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/dispatch/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehiclesJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        mockMvc.perform(post("/api/dispatch/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ordersJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispatchPlan[0].vehicleId").value("VEH001"))
                .andExpect(jsonPath("$.dispatchPlan[0].totalLoad").value(25.0))
                .andExpect(jsonPath("$.dispatchPlan[0].assignedOrders", hasSize(1)))
                .andExpect(jsonPath("$.dispatchPlan[0].assignedOrders[0].orderId").value("ORD_HIGH"))
                .andExpect(jsonPath("$.unassignedOrders", hasSize(1)))
                .andExpect(jsonPath("$.unassignedOrders[0].orderId").value("ORD_LOW"));
    }

    @Test
    void shouldUpsertExistingOrdersAndVehiclesWithoutDuplication() throws Exception {
        String vehiclePayload = """
                {
                  "vehicles": [
                    {
                      "vehicleId": "VEH001",
                      "capacity": 100,
                      "currentLatitude": 12.9716,
                      "currentLongitude": 77.6413,
                      "currentAddress": "Indiranagar, Bangalore"
                    }
                  ]
                }
                """;

        String initialOrder = """
                {
                  "orders": [
                    {
                      "orderId": "ORD001",
                      "latitude": 12.9716,
                      "longitude": 77.5946,
                      "address": "MG Road, Bangalore",
                      "packageWeight": 10,
                      "priority": "MEDIUM"
                    }
                  ]
                }
                """;

        String updatedOrder = """
                {
                  "orders": [
                    {
                      "orderId": "ORD001",
                      "latitude": 12.9716,
                      "longitude": 77.5946,
                      "address": "MG Road, Bangalore",
                      "packageWeight": 35,
                      "priority": "HIGH"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/dispatch/vehicles").contentType(MediaType.APPLICATION_JSON).content(vehiclePayload))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON).content(initialOrder))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON).content(updatedOrder))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispatchPlan[0].assignedOrders", hasSize(1)))
                .andExpect(jsonPath("$.dispatchPlan[0].totalLoad").value(35.0))
                .andExpect(jsonPath("$.dispatchPlan[0].assignedOrders[0].priority").value("HIGH"));
    }

    @Test
    void shouldReturnBadRequestForInvalidInputsAndMissingFleetState() throws Exception {
        // Requesting plan before adding vehicles/orders
        mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        // Invalid latitude and negative weight
        String invalidOrder = """
                {
                  "orders": [
                    {
                      "orderId": "BAD01",
                      "latitude": 145.0,
                      "longitude": 77.5946,
                      "address": "Invalid Location",
                      "packageWeight": -5,
                      "priority": "HIGH"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/dispatch/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidOrder))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void shouldOptimizeFullThirtyOrderDelhiNcrDatasetRespectingConstraints() throws Exception {
        String vehiclesJson = """
                {
                  "vehicles": [
                    { "vehicleId": "VEH001", "capacity": 100, "currentLatitude": 28.7041, "currentLongitude": 77.1025, "currentAddress": "Karol Bagh, Delhi, India" },
                    { "vehicleId": "VEH002", "capacity": 80, "currentLatitude": 28.5355, "currentLongitude": 77.3910, "currentAddress": "Sector 18, Noida, Uttar Pradesh, India" },
                    { "vehicleId": "VEH003", "capacity": 120, "currentLatitude": 28.4595, "currentLongitude": 77.0266, "currentAddress": "Cyber Hub, Gurgaon, Haryana, India" },
                    { "vehicleId": "VEH004", "capacity": 90, "currentLatitude": 28.6139, "currentLongitude": 77.2090, "currentAddress": "Connaught Place, Delhi, India" },
                    { "vehicleId": "VEH005", "capacity": 110, "currentLatitude": 28.7041, "currentLongitude": 77.1025, "currentAddress": "Karol Bagh, Delhi, India" }
                  ]
                }
                """;

        String thirtyOrdersJson = """
                {
                  "orders": [
                    { "orderId": "ORD001", "latitude": 28.6139, "longitude": 77.2090, "address": "Connaught Place, Delhi, India", "packageWeight": 15, "priority": "HIGH" },
                    { "orderId": "ORD002", "latitude": 28.6139, "longitude": 77.2090, "address": "Connaught Place, Delhi, India", "packageWeight": 10, "priority": "MEDIUM" },
                    { "orderId": "ORD003", "latitude": 28.7041, "longitude": 77.1025, "address": "Karol Bagh, Delhi, India", "packageWeight": 20, "priority": "LOW" },
                    { "orderId": "ORD004", "latitude": 28.5355, "longitude": 77.3910, "address": "Sector 18, Noida, Uttar Pradesh, India", "packageWeight": 25, "priority": "HIGH" },
                    { "orderId": "ORD005", "latitude": 28.4595, "longitude": 77.0266, "address": "Cyber Hub, Gurgaon, Haryana, India", "packageWeight": 30, "priority": "MEDIUM" },
                    { "orderId": "ORD006", "latitude": 28.6139, "longitude": 77.2090, "address": "Connaught Place, Delhi, India", "packageWeight": 40, "priority": "LOW" },
                    { "orderId": "ORD007", "latitude": 28.7041, "longitude": 77.1025, "address": "Karol Bagh, Delhi, India", "packageWeight": 20, "priority": "HIGH" },
                    { "orderId": "ORD008", "latitude": 28.5355, "longitude": 77.3910, "address": "Sector 18, Noida, Uttar Pradesh, India", "packageWeight": 25, "priority": "HIGH" },
                    { "orderId": "ORD009", "latitude": 28.4595, "longitude": 77.0266, "address": "Cyber Hub, Gurgaon, Haryana, India", "packageWeight": 15, "priority": "MEDIUM" },
                    { "orderId": "ORD010", "latitude": 28.6139, "longitude": 77.2090, "address": "Connaught Place, Delhi, India", "packageWeight": 30, "priority": "LOW" },
                    { "orderId": "ORD011", "latitude": 28.7041, "longitude": 77.1025, "address": "Karol Bagh, Delhi, India", "packageWeight": 20, "priority": "HIGH" },
                    { "orderId": "ORD012", "latitude": 28.5355, "longitude": 77.3910, "address": "Sector 18, Noida, Uttar Pradesh, India", "packageWeight": 10, "priority": "MEDIUM" },
                    { "orderId": "ORD013", "latitude": 28.4595, "longitude": 77.0266, "address": "Cyber Hub, Gurgaon, Haryana, India", "packageWeight": 25, "priority": "HIGH" },
                    { "orderId": "ORD014", "latitude": 28.6139, "longitude": 77.2090, "address": "Connaught Place, Delhi, India", "packageWeight": 15, "priority": "LOW" },
                    { "orderId": "ORD015", "latitude": 28.7041, "longitude": 77.1025, "address": "Karol Bagh, Delhi, India", "packageWeight": 20, "priority": "HIGH" },
                    { "orderId": "ORD016", "latitude": 28.5355, "longitude": 77.3910, "address": "Sector 18, Noida, Uttar Pradesh, India", "packageWeight": 30, "priority": "LOW" },
                    { "orderId": "ORD017", "latitude": 28.4595, "longitude": 77.0266, "address": "Cyber Hub, Gurgaon, Haryana, India", "packageWeight": 15, "priority": "MEDIUM" },
                    { "orderId": "ORD018", "latitude": 28.6139, "longitude": 77.2090, "address": "Connaught Place, Delhi, India", "packageWeight": 10, "priority": "HIGH" },
                    { "orderId": "ORD019", "latitude": 28.7041, "longitude": 77.1025, "address": "Karol Bagh, Delhi, India", "packageWeight": 20, "priority": "LOW" },
                    { "orderId": "ORD020", "latitude": 28.5355, "longitude": 77.3910, "address": "Sector 18, Noida, Uttar Pradesh, India", "packageWeight": 30, "priority": "HIGH" },
                    { "orderId": "ORD021", "latitude": 28.6139, "longitude": 77.2090, "address": "Connaught Place, Delhi, India", "packageWeight": 25, "priority": "MEDIUM" },
                    { "orderId": "ORD022", "latitude": 28.7041, "longitude": 77.1025, "address": "Karol Bagh, Delhi, India", "packageWeight": 20, "priority": "LOW" },
                    { "orderId": "ORD023", "latitude": 28.5355, "longitude": 77.3910, "address": "Sector 18, Noida, Uttar Pradesh, India", "packageWeight": 30, "priority": "HIGH" },
                    { "orderId": "ORD024", "latitude": 28.4595, "longitude": 77.0266, "address": "Cyber Hub, Gurgaon, Haryana, India", "packageWeight": 15, "priority": "LOW" },
                    { "orderId": "ORD025", "latitude": 28.6139, "longitude": 77.2090, "address": "Connaught Place, Delhi, India", "packageWeight": 20, "priority": "MEDIUM" },
                    { "orderId": "ORD026", "latitude": 28.7041, "longitude": 77.1025, "address": "Karol Bagh, Delhi, India", "packageWeight": 25, "priority": "HIGH" },
                    { "orderId": "ORD027", "latitude": 28.5355, "longitude": 77.3910, "address": "Sector 18, Noida, Uttar Pradesh, India", "packageWeight": 20, "priority": "MEDIUM" },
                    { "orderId": "ORD028", "latitude": 28.4595, "longitude": 77.0266, "address": "Cyber Hub, Gurgaon, Haryana, India", "packageWeight": 15, "priority": "LOW" },
                    { "orderId": "ORD029", "latitude": 28.6139, "longitude": 77.2090, "address": "Connaught Place, Delhi, India", "packageWeight": 30, "priority": "HIGH" },
                    { "orderId": "ORD030", "latitude": 28.7041, "longitude": 77.1025, "address": "Karol Bagh, Delhi, India", "packageWeight": 20, "priority": "LOW" }
                  ]
                }
                """;

        mockMvc.perform(post("/api/dispatch/vehicles").contentType(MediaType.APPLICATION_JSON).content(vehiclesJson))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON).content(thirtyOrdersJson))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispatchPlan", hasSize(5)))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        int totalAccountedOrders = root.get("unassignedOrders").size();
        int highPriorityAssigned = 0;

        for (JsonNode vehiclePlan : root.get("dispatchPlan")) {
            String vehicleId = vehiclePlan.get("vehicleId").asText();
            double totalLoad = vehiclePlan.get("totalLoad").asDouble();
            double maxCapacity = switch (vehicleId) {
                case "VEH001" -> 100.0;
                case "VEH002" -> 80.0;
                case "VEH003" -> 120.0;
                case "VEH004" -> 90.0;
                case "VEH005" -> 110.0;
                default -> 0.0;
            };
            assertTrue(totalLoad <= maxCapacity, vehicleId + " exceeded capacity");

            for (JsonNode order : vehiclePlan.get("assignedOrders")) {
                totalAccountedOrders++;
                if ("HIGH".equals(order.get("priority").asText())) {
                    highPriorityAssigned++;
                }
            }
        }

        // All 30 orders are accounted for, and all 12 HIGH-priority orders are assigned
        assertEquals(30, totalAccountedOrders);
        assertEquals(12, highPriorityAssigned);
    }
}
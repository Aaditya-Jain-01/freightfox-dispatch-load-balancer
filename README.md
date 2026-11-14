# FreightFox Dispatch Load Balancer

RESTful Spring Boot 3 (Java 21) service that optimizes delivery order allocation across a vehicle fleet using Haversine great-circle distances, strict priority scheduling (`HIGH` -> `MEDIUM` -> `LOW`), and capacity-constrained nearest-neighbor route sequencing.

## Architecture & Design Decisions
- **Strategy Pattern (`DispatchOptimizationStrategy`)**: Decouples the routing algorithm (`PriorityNearestNeighborStrategy`) from REST controllers and persistence services so solvers can be extended or swapped cleanly.
- **Idempotent Upsert Persistence**: `DeliveryOrder` and `Vehicle` entities use business identifiers (`orderId`, `vehicleId`) as primary keys with batch deduplication prior to `saveAll`.
- **Priority & Capacity Guarantees**: Orders are scheduled in strict `HIGH -> MEDIUM -> LOW` priority tiers, selecting the closest feasible vehicle within capacity limits and re-sequencing each vehicle's assigned stops via nearest-neighbor tour construction. Orders that cannot fit within remaining fleet capacity are returned explicitly under `unassignedOrders`.
- **Interactive API Documentation**: OpenAPI 3 / Swagger UI is served at `/swagger-ui.html` (with `/` redirecting automatically).

## API Endpoints
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/dispatch/orders` | Ingest or upsert a batch of delivery orders |
| `POST` | `/api/dispatch/vehicles` | Ingest or upsert a batch of fleet vehicles |
| `GET` | `/api/dispatch/plan` | Compute and return the optimized fleet dispatch plan |
| `DELETE` | `/api/dispatch/reset` | Clear all persisted orders and vehicles |
| `GET` | `/health` | Service health check |

## Running Locally
```bash
mvn spring-boot:run
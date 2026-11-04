# Stark Group VMS - Backend (Assignment 2 Fleet Management)

REST API and backend service layer for the **Stark Group Vehicle Management System (VMS)**, built with **Spring Boot 3.4.3**, **Spring Security 6**, **Spring Data JPA**, **Flyway**, and **PostgreSQL**.

---

## 1. Implemented REST APIs

All endpoints (except `/api/auth/login` and `/actuator/health`) require a valid Bearer JWT in the `Authorization` header.

### Core Vehicle Endpoints
| Method | Endpoint | Description | Status Codes |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Authenticate course user and obtain Bearer JWT token | 200 OK, 401 Unauthorized, 400 Bad Request |
| `GET` | `/api/vehicles` | List or paginate/filter vehicles (`page`, `size`, `search`, `status`, `company`, `sortBy`, `sortDir`) | 200 OK, 401 Unauthorized |
| `GET` | `/api/vehicles/{id}` | Retrieve details of a single vehicle | 200 OK, 401 Unauthorized, 404 Not Found |
| `POST` | `/api/vehicles` | Register a new vehicle | 201 Created, 400 Bad Request, 401 Unauthorized, 409 Conflict |
| `PUT` | `/api/vehicles/{id}` | Update existing vehicle attributes | 200 OK, 400 Bad Request, 401 Unauthorized, 404 Not Found, 409 Conflict |
| `PATCH` | `/api/vehicles/{id}/status` | Update vehicle status (`ACTIVE`, `INACTIVE`, `MAINTENANCE`) | 200 OK, 400 Bad Request, 404 Not Found |
| `POST` | `/api/vehicles/{id}/assign-driver/{driverId}` | Assign or change driver for vehicle | 200 OK, 404 Not Found |
| `POST` | `/api/vehicles/{id}/unassign-driver` | Unassign driver from vehicle | 200 OK, 404 Not Found |
| `DELETE` | `/api/vehicles/{id}` | Remove vehicle from fleet registry | 204 No Content, 401 Unauthorized, 404 Not Found |

### Driver Management Endpoints
| Method | Endpoint | Description | Status Codes |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/drivers` | Search and paginate drivers | 200 OK, 401 Unauthorized |
| `GET` | `/api/drivers/available` | List unassigned drivers available for vehicle assignment | 200 OK, 401 Unauthorized |
| `GET` | `/api/drivers/{id}` | Get driver details | 200 OK, 404 Not Found |
| `POST` | `/api/drivers` | Create new driver (enforces unique license number) | 201 Created, 409 Conflict |
| `PUT` | `/api/drivers/{id}` | Update driver attributes | 200 OK, 404 Not Found, 409 Conflict |
| `DELETE` | `/api/drivers/{id}` | Remove driver record | 204 No Content, 404 Not Found |

### Document & Maintenance Endpoints
| Method | Endpoint | Description | Status Codes |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/vehicles/{id}/documents` | Retrieve all documents for vehicle with calculated expiry status | 200 OK, 404 Not Found |
| `POST` | `/api/vehicles/{id}/documents` | Upload new vehicle document (Registration, Tax Token, Insurance, Fitness) | 201 Created, 400 Bad Request |
| `DELETE` | `/api/vehicles/{id}/documents/{docId}` | Remove vehicle document | 204 No Content, 404 Not Found |
| `GET` | `/api/vehicles/{id}/maintenance` | Chronological maintenance records and history | 200 OK, 404 Not Found |
| `POST` | `/api/vehicles/{id}/maintenance` | Log maintenance/service record (date, type, cost, workshop) | 201 Created, 400 Bad Request |
| `DELETE` | `/api/vehicles/{id}/maintenance/{recordId}`| Delete maintenance record | 204 No Content, 404 Not Found |

### Dashboard & Analytics Endpoint
| Method | Endpoint | Description | Status Codes |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/dashboard/stats` | Aggregated fleet KPIs, status breakdowns, and upcoming renewals | 200 OK, 401 Unauthorized |
| `GET` | `/actuator/health` | Application and PostgreSQL connectivity health | 200 OK |

---

## 2. Prerequisites & Technology Stack

- **Java Development Kit (JDK):** Version 17+ (Built and tested on OpenJDK 26 / 21 LTS)
- **Database:** PostgreSQL 14+
- **Build Tool:** Gradle 9.3.1 (provided via `gradlew` wrapper)

---

## 3. Database & Migrations

The database schema is managed automatically by **Flyway**:
- `V1__create_vehicles_table.sql`: Creates `vehicles` table with unique constraint on vehicle numbers.
- `V2__seed_course_user.sql`: Creates `users` table with course credential seed.
- `V3__create_drivers_and_associations.sql`: Creates `drivers` table (`license_number` UNIQUE) and assigns driver foreign key to `vehicles`.
- `V4__create_documents_and_maintenance_tables.sql`: Creates `vehicle_documents` and `vehicle_maintenance_records` tables.

---

## 4. Configuration & Environment Variables

All sensitive and environment-specific settings are strictly externalized.

| Variable | Description | Example / Default |
| :--- | :--- | :--- |
| `DB_URL` | JDBC connection URL to PostgreSQL | `jdbc:postgresql://localhost:5432/stark_vms_db` |
| `DB_USERNAME` | PostgreSQL database user | `vms_user` |
| `DB_PASSWORD` | PostgreSQL database password | *(empty or your secret)* |
| `JWT_SECRET` | Secret key for HMAC-SHA256 signing (minimum 32 bytes) | *(32+ character secret)* |
| `JWT_EXPIRATION_SECONDS` | Token lifetime | `900` (15 minutes) |
| `COURSE_USER_EMAIL` | Email for course authentication user | `test@starkgroup.test` |
| `COURSE_USER_PASSWORD_HASH` | BCrypt password hash for user | *(BCrypt hash)* |
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed frontend origins | `http://localhost:5173,https://your-site.netlify.app` |

Template file available at `.env.example`.

---

## 5. Build, Test, and Run Commands

### Run Unit and Integration Tests
```bash
./gradlew test --no-daemon
```

### Build Executable JAR
```bash
./gradlew bootJar --no-daemon
```

---

## 6. Docker & Render Deployment

The backend includes a multi-stage `Dockerfile` and declarative Render blueprint (`render.yaml`).
- Built with Java 21 LTS (`eclipse-temurin:21-jre-jammy`).
- Runs as a non-root system user (`appuser:1001`).
- Binds dynamically to Render's allocated `$PORT` on `0.0.0.0`.
- Health check: `/actuator/health`.

---

## 7. Associated Frontend Repository

The Flutter Web frontend is maintained in:
- **vms-frontend-a2:** [https://github.com/17324Mahir/vms-frontend-a2](https://github.com/17324Mahir/vms-frontend-a2)

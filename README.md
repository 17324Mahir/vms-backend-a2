# Stark Group VMS - Backend

REST API and backend service layer for the **Stark Group Vehicle Management System (VMS)**, built with **Spring Boot 3.4.3**, **Spring Security 6**, **Spring Data JPA**, **Flyway**, and **PostgreSQL**.

---

## 1. Implemented REST APIs

All endpoints (except `/api/auth/login` and `/actuator/health`) require a valid Bearer JWT in the `Authorization` header.

| Method | Endpoint | Description | Status Codes |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Authenticate course user and obtain Bearer JWT token | 200 OK, 401 Unauthorized, 400 Bad Request |
| `GET` | `/api/vehicles` | List all registered vehicles in fleet | 200 OK, 401 Unauthorized |
| `GET` | `/api/vehicles/{id}` | Retrieve details of a single vehicle | 200 OK, 401 Unauthorized, 404 Not Found |
| `POST` | `/api/vehicles` | Register a new vehicle | 201 Created, 400 Bad Request, 401 Unauthorized, 409 Conflict |
| `PUT` | `/api/vehicles/{id}` | Update existing vehicle attributes | 200 OK, 400 Bad Request, 401 Unauthorized, 404 Not Found, 409 Conflict |
| `DELETE` | `/api/vehicles/{id}` | Remove vehicle from fleet registry | 204 No Content, 401 Unauthorized, 404 Not Found |
| `GET` | `/actuator/health` | Application and PostgreSQL connectivity health | 200 OK |

Full API specification and schema contracts are documented in [`docs/vehicle-api.md`](docs/vehicle-api.md).

---

## 2. Prerequisites & Technology Stack

- **Java Development Kit (JDK):** Version 17+ (Built and tested on OpenJDK 26 / 21 LTS)
- **Database:** PostgreSQL 14+
- **Build Tool:** Gradle 9.3.1 (provided via `gradlew` wrapper)

---

## 3. Database & Migrations

The database schema is managed automatically by **Flyway**:
- Migration script: `src/main/resources/db/migration/V1__create_vehicles_table.sql`
- Creates table `vehicles` with case-insensitive unique constraint on vehicle numbers (`uq_vehicles_vehicle_number_upper`).

Before running the application, ensure your PostgreSQL database exists:
```sql
CREATE DATABASE stark_vms_db;
CREATE USER vms_user WITH ENCRYPTED PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE stark_vms_db TO vms_user;
```

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
./gradlew build -x test --no-daemon
```

### Run Application Locally
```bash
DB_URL="jdbc:postgresql://localhost:5432/stark_vms_db" \
DB_USERNAME="vms_user" \
DB_PASSWORD="" \
JWT_SECRET="your-32-byte-or-longer-secret-key-here!" \
COURSE_USER_EMAIL="test@starkgroup.test" \
COURSE_USER_PASSWORD_HASH='$2a$10$zfYD.tKC4HUNa59nE9UIReikpP660ZT5Yg.nkmbkpEk./PCUyupC2' \
CORS_ALLOWED_ORIGINS="http://localhost:5173" \
./gradlew bootRun --no-daemon
```

---

## 6. Docker & Render Deployment

The backend includes a multi-stage `Dockerfile` and a declarative Render blueprint (`render.yaml`).
- Built with Java 21 LTS (`eclipse-temurin:21-jre-jammy`).
- Runs as a non-root system user (`appuser:1001`).
- Binds dynamically to Render's allocated `$PORT` on `0.0.0.0`.
- Health check: `/actuator/health`.

For full deployment steps, database setup, and environment variable configuration on Render, see [`docs/render-deployment.md`](docs/render-deployment.md).

---

## 7. Associated Frontend Repository

The Flutter Web frontend is maintained in:
- **vms-frontend:** [https://github.com/17324Mahir/vms-frontend](https://github.com/17324Mahir/vms-frontend)

# Stark Group VMS - Backend Render Deployment Guide

This guide details the deployment of the **Stark Group VMS Spring Boot backend** as a Dockerized Web Service on [Render](https://render.com).

---

## 1. Architecture & Deployment Strategy

- **Runtime:** Docker (Multi-stage build using Eclipse Temurin Java 21 LTS)
- **Service Type:** Render Web Service
- **Plan:** Free
- **Health Check Path:** `/actuator/health`
- **Port:** Configured via `server.port=${PORT:8080}` to bind dynamically to Render's allocated `$PORT` on `0.0.0.0`.
- **Security:** Runs as non-root user (`appuser:1001`), with PID 1 signal forwarding for graceful shutdown.

---

## 2. PostgreSQL Database on Render

Render provides managed PostgreSQL instances. When deploying:

1. **Create Database:** In the Render dashboard, click **New > PostgreSQL Database** (Free Plan).
2. **Obtain Connection Details:**
   - Under **Connections**, find the **Internal Database URL** (for services hosted in the same Render region) or **External Database URL**.
   - Standard Render format: `postgres://[user]:[password]@[hostname]/[database]`
3. **Convert to Spring Boot JDBC Format:**
   - Spring Boot requires standard JDBC connection parameters:
     - `DB_URL`: `jdbc:postgresql://<hostname>:5432/<database>?sslmode=require`
     - `DB_USERNAME`: `<user>`
     - `DB_PASSWORD`: `<password>`
   - Do NOT pass the raw `postgres://` URL directly into `DB_URL`.

---

## 3. Environment Variables Configuration

Configure the following variables in the Render Dashboard (**vms-backend > Environment**) or via Blueprint (`render.yaml`):

| Variable | Required | Description | Example Value |
| :--- | :--- | :--- | :--- |
| `PORT` | Auto | Assigned automatically by Render | `8080` |
| `DB_URL` | **Yes** | PostgreSQL JDBC connection URL | `jdbc:postgresql://dpg-xxx.oregon-postgres.render.com:5432/stark_vms?sslmode=require` |
| `DB_USERNAME` | **Yes** | Database username | `vms_user` |
| `DB_PASSWORD` | **Yes** | Database password | *(secret)* |
| `JWT_SECRET` | **Yes** | Minimum 32-character (256-bit) secret for HMAC-SHA256 | *(generate in dashboard or use 32+ char key)* |
| `JWT_EXPIRATION_SECONDS` | No | Token expiration time (default: 900s = 15m) | `900` |
| `JWT_ISSUER` | No | Token issuer (default: `stark-vms-backend`) | `stark-vms-backend` |
| `JWT_AUDIENCE` | No | Token audience (default: `stark-vms-frontend`) | `stark-vms-frontend` |
| `COURSE_USER_EMAIL` | **Yes** | Login email for course user | `test@starkgroup.test` |
| `COURSE_USER_PASSWORD_HASH` | **Yes** | BCrypt hash for course user password | `$2a$10$zfYD.tKC4HUNa59nE9UIReikpP660ZT5Yg.nkmbkpEk./PCUyupC2` |
| `CORS_ALLOWED_ORIGINS` | **Yes** | Exact frontend origins (comma-separated, no trailing slash) | `https://your-frontend.netlify.app,http://localhost:5173` |

---

## 4. Deploying via Render Dashboard

1. Log in to [dashboard.render.com](https://dashboard.render.com).
2. Click **New + > Web Service**.
3. Connect your GitHub account and select **`17324Mahir/vms-backend`**.
4. Configure service details:
   - **Name:** `vms-backend`
   - **Branch:** `main`
   - **Runtime:** `Docker` (Render automatically uses the repository's `Dockerfile`)
   - **Health Check Path:** `/actuator/health`
5. Add the required Environment Variables from the table above.
6. Click **Create Web Service**.

---

## 5. Verification After Deployment

Once the build and deployment succeed:

### 5.1 Health Check
```bash
curl -i https://<your-service>.onrender.com/actuator/health
# Expected: HTTP 200 OK
# Response: {"status":"UP"}
```

### 5.2 Unauthenticated Route Rejection
```bash
curl -i https://<your-service>.onrender.com/api/vehicles
# Expected: HTTP 401 Unauthorized
```

### 5.3 Authentication
```bash
curl -i -X POST https://<your-service>.onrender.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@starkgroup.test","password":"SyntheticTestPassword@123"}'
# Expected: HTTP 200 OK with {"token":"Bearer ..."}
```

### 5.4 Preflight CORS Check
```bash
curl -i -X OPTIONS https://<your-service>.onrender.com/api/vehicles \
  -H "Origin: https://your-frontend.netlify.app" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization"
# Expected: Access-Control-Allow-Origin: https://your-frontend.netlify.app
```

---

## 6. Troubleshooting

| Issue | Cause | Solution |
| :--- | :--- | :--- |
| `Connection to localhost:5432 refused` | `DB_URL` still pointing to local host | Update `DB_URL` in Render Environment settings to your Render PostgreSQL host. |
| `FATAL: no pg_hba.conf entry for host ... no encryption` | SSL/TLS connection required by provider | Ensure `?sslmode=require` is appended to the JDBC URL. |
| Health check timeout | Slow initial cold start on Free tier | Free instances spin down when inactive. Render allows up to a few minutes on initial startup. |
| `403 Forbidden` / CORS blocked in browser | Netlify origin not listed in backend CORS | Update `CORS_ALLOWED_ORIGINS` to include the exact frontend URL scheme and domain. |

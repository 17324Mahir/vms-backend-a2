# Stage 1: Build the Spring Boot application using Java 21 LTS
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

# Copy Gradle wrapper and build configuration first for layer caching
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew

# Download dependencies (fail-safe cache priming)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code and build deterministic production artifact
COPY src/ src/
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Minimal, secure runtime image
FROM eclipse-temurin:21-jre-jammy

# Create a non-root system user and group
RUN groupadd -r appgroup && useradd -r -g appgroup -u 1001 appuser

WORKDIR /app

# Copy deterministic executable JAR from builder stage
COPY --from=builder --chown=appuser:appgroup /workspace/build/libs/app.jar /app/app.jar

USER appuser

EXPOSE 8080

# Exec-style startup ensuring POSIX signals reach JVM
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]

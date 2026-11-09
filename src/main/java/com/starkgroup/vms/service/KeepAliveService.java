package com.starkgroup.vms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.time.Duration;

/**
 * Proactive Keep-Alive & Warmup Service for Stark Group VMS (Assignment 2).
 *
 * Prevents Render's free-tier container from spinning down after 15 minutes of inactivity:
 * 1. Proactively dispatches an external HTTPS GET request to the backend's public Render URL
 *    (https://vms-backend-a2.onrender.com/actuator/health) every 7 minutes.
 *    Because this request transits through Render's public edge router / reverse proxy, Render
 *    registers incoming HTTP traffic and resets its 15-minute inactivity countdown.
 * 2. Continuously exercises the HikariCP database connection pool to ensure connections remain
 *    fresh and warm, eliminating PostgreSQL connection initialization spikes on user requests.
 */
@Service
public class KeepAliveService {

    private static final Logger log = LoggerFactory.getLogger(KeepAliveService.class);

    private final HttpClient httpClient;
    private final DataSource dataSource;

    @Value("${app.keep-alive.enabled:true}")
    private boolean enabled;

    @Value("${app.keep-alive.target-url:https://vms-backend-a2.onrender.com/actuator/health}")
    private String targetUrl;

    public KeepAliveService(@Autowired(required = false) DataSource dataSource) {
        this.dataSource = dataSource;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Scheduled keep-alive trigger: default rate 420,000 ms (7 minutes), initial delay 30,000 ms.
     */
    @Scheduled(
            fixedRateString = "${app.keep-alive.interval-ms:420000}",
            initialDelayString = "${app.keep-alive.initial-delay-ms:30000}"
    )
    public void pingKeepAlive() {
        if (!enabled) {
            log.debug("KeepAliveService is disabled in current environment.");
            return;
        }

        pingExternalUrl();
        warmupDatabase();
    }

    /**
     * Performs an outbound HTTP GET request to the target public URL.
     *
     * @return true if ping completed with 2xx/3xx HTTP status code, false otherwise.
     */
    public boolean pingExternalUrl() {
        if (targetUrl == null || targetUrl.isBlank()) {
            log.warn("Keep-alive target URL is empty; skipping external HTTP ping.");
            return false;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl.trim()))
                    .timeout(Duration.ofSeconds(25))
                    .header("User-Agent", "Stark-VMS-A2-KeepAlive/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode >= 200 && statusCode < 400) {
                log.info("Keep-alive ping SUCCESS -> {} [HTTP {}]", targetUrl, statusCode);
                return true;
            } else {
                log.warn("Keep-alive ping returned non-2xx status -> {} [HTTP {}]", targetUrl, statusCode);
                return false;
            }
        } catch (Exception e) {
            log.warn("Keep-alive ping to {} encountered an error: {}", targetUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Exercises the database connection pool by validating an active connection.
     *
     * @return true if connection is valid, false otherwise.
     */
    public boolean warmupDatabase() {
        if (dataSource == null) {
            return false;
        }
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(3);
            log.debug("Keep-alive database connection validation: {}", valid ? "VALID" : "INVALID");
            return valid;
        } catch (Exception e) {
            log.warn("Keep-alive database warmup encountered an error: {}", e.getMessage());
            return false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }
}

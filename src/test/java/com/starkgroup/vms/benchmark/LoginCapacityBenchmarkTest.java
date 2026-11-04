package com.starkgroup.vms.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starkgroup.vms.domain.User;
import com.starkgroup.vms.dto.LoginRequest;
import com.starkgroup.vms.dto.LoginResponse;
import com.starkgroup.vms.repository.UserRepository;
import com.starkgroup.vms.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Controlled load and concurrency benchmark for Stark Group VMS authentication and fleet endpoints.
 *
 * Measures:
 * 1. BCrypt(10) CPU cost profile (baseline computational limit).
 * 2. Successful login throughput and latency under concurrency (1, 5, 10, 20).
 * 3. Invalid login throughput and latency under concurrency (dummy hash timing defense).
 * 4. Authenticated vehicle query throughput and latency under concurrency (pool & JWT validation).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoginCapacityBenchmarkTest {

    private static final Logger log = LoggerFactory.getLogger(LoginCapacityBenchmarkTest.class);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BENCHMARK_EMAIL = "benchmark.user@starkgroup.test";
    private static final String BENCHMARK_PASSWORD = "BenchmarkPassword@123";

    private String baseUrl;
    private String validToken;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        if (!userRepository.existsByEmailIgnoreCase(BENCHMARK_EMAIL)) {
            userRepository.save(new User(
                    "Benchmark User",
                    BENCHMARK_EMAIL,
                    passwordEncoder.encode(BENCHMARK_PASSWORD),
                    "USER"
            ));
        }
        validToken = jwtService.generateToken(BENCHMARK_EMAIL);
    }

    @AfterEach
    void tearDown() {
        userRepository.findByEmailIgnoreCase(BENCHMARK_EMAIL).ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("Execute controlled authentication & API capacity benchmark")
    void runControlledCapacityBenchmark() throws Exception {
        log.info("===============================================================================");
        log.info("STARTING CONTROLLED VMS CAPACITY & AUTHENTICATION BENCHMARK");
        log.info("Server port: {}, Base URL: {}", port, baseUrl);
        log.info("===============================================================================");

        // 1. Direct BCrypt Cost Measurement (10 rounds)
        long bcryptStart = System.nanoTime();
        final int bcryptSamples = 10;
        for (int i = 0; i < bcryptSamples; i++) {
            passwordEncoder.matches("TestCandidatePassword", "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi");
        }
        long bcryptDurationMs = (System.nanoTime() - bcryptStart) / (1_000_000 * bcryptSamples);
        log.info("[BCRYPT BASELINE] Single-thread cost factor 10 verification: ~{} ms/op", bcryptDurationMs);

        // 2. Controlled Concurrency Benchmarks across scenarios
        int[] concurrencyLevels = {1, 5, 10, 20};
        int requestsPerWorker = 5;

        for (int concurrency : concurrencyLevels) {
            int totalRequests = concurrency * requestsPerWorker;

            // Scenario A: Successful Login
            BenchmarkResult successLoginResult = runConcurrentRequests(
                    concurrency,
                    totalRequests,
                    () -> {
                        LoginRequest req = new LoginRequest(BENCHMARK_EMAIL, BENCHMARK_PASSWORD);
                        ResponseEntity<String> resp = restTemplate.postForEntity(
                                baseUrl + "/api/auth/login",
                                new HttpEntity<>(req),
                                String.class
                        );
                        return resp.getStatusCode() == HttpStatus.OK;
                    }
            );

            // Scenario B: Invalid Login (exercising timing attack protection dummy hash)
            BenchmarkResult invalidLoginResult = runConcurrentRequests(
                    concurrency,
                    totalRequests,
                    () -> {
                        LoginRequest req = new LoginRequest(BENCHMARK_EMAIL, "WrongPassword@999");
                        ResponseEntity<String> resp = restTemplate.postForEntity(
                                baseUrl + "/api/auth/login",
                                new HttpEntity<>(req),
                                String.class
                        );
                        return resp.getStatusCode() == HttpStatus.UNAUTHORIZED;
                    }
            );

            // Scenario C: Authenticated Vehicle List (exercising in-memory JWT + Hikari pool)
            BenchmarkResult vehicleResult = runConcurrentRequests(
                    concurrency,
                    totalRequests,
                    () -> {
                        HttpHeaders headers = new HttpHeaders();
                        headers.setBearerAuth(validToken);
                        ResponseEntity<String> resp = restTemplate.exchange(
                                baseUrl + "/api/vehicles",
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                String.class
                        );
                        return resp.getStatusCode() == HttpStatus.OK;
                    }
            );

            log.info("-------------------------------------------------------------------------------");
            log.info("CONCURRENCY LEVEL: {} (Total: {} requests)", concurrency, totalRequests);
            log.info("  A. Valid Login:        throughput={} req/s, p50={} ms, p95={} ms, p99={} ms, errors={}",
                    String.format(Locale.ROOT, "%.2f", successLoginResult.throughputRps),
                    successLoginResult.p50Ms, successLoginResult.p95Ms, successLoginResult.p99Ms, successLoginResult.errorCount);
            log.info("  B. Invalid Login:      throughput={} req/s, p50={} ms, p95={} ms, p99={} ms, errors={}",
                    String.format(Locale.ROOT, "%.2f", invalidLoginResult.throughputRps),
                    invalidLoginResult.p50Ms, invalidLoginResult.p95Ms, invalidLoginResult.p99Ms, invalidLoginResult.errorCount);
            log.info("  C. Authenticated Read: throughput={} req/s, p50={} ms, p95={} ms, p99={} ms, errors={}",
                    String.format(Locale.ROOT, "%.2f", vehicleResult.throughputRps),
                    vehicleResult.p50Ms, vehicleResult.p95Ms, vehicleResult.p99Ms, vehicleResult.errorCount);
        }
        log.info("===============================================================================");
    }

    private BenchmarkResult runConcurrentRequests(int concurrency, int totalRequests, Callable<Boolean> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>(totalRequests));
        int errorCount = 0;

        long startTime = System.nanoTime();
        List<Future<RequestOutcome>> futures = new ArrayList<>();

        for (int i = 0; i < totalRequests; i++) {
            futures.add(executor.submit(() -> {
                long reqStart = System.nanoTime();
                boolean success = false;
                try {
                    success = task.call();
                } catch (Exception e) {
                    success = false;
                }
                long durationMs = (System.nanoTime() - reqStart) / 1_000_000;
                return new RequestOutcome(durationMs, success);
            }));
        }

        for (Future<RequestOutcome> future : futures) {
            try {
                RequestOutcome outcome = future.get(30, TimeUnit.SECONDS);
                latencies.add(outcome.durationMs);
                if (!outcome.success) {
                    errorCount++;
                }
            } catch (Exception e) {
                errorCount++;
            }
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        long totalDurationMs = (System.nanoTime() - startTime) / 1_000_000;
        double throughputRps = totalDurationMs > 0 ? (totalRequests * 1000.0) / totalDurationMs : 0.0;

        Collections.sort(latencies);
        long p50 = latencies.isEmpty() ? 0 : latencies.get((int) (latencies.size() * 0.50));
        long p95 = latencies.isEmpty() ? 0 : latencies.get((int) (latencies.size() * 0.95));
        long p99 = latencies.isEmpty() ? 0 : latencies.get(Math.min(latencies.size() - 1, (int) (latencies.size() * 0.99)));

        return new BenchmarkResult(throughputRps, p50, p95, p99, errorCount, totalDurationMs);
    }

    record RequestOutcome(long durationMs, boolean success) {}

    record BenchmarkResult(
            double throughputRps,
            long p50Ms,
            long p95Ms,
            long p99Ms,
            int errorCount,
            long totalDurationMs
    ) {}
}

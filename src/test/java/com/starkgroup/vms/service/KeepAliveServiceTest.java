package com.starkgroup.vms.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class KeepAliveServiceTest {

    @Test
    @DisplayName("pingKeepAlive should do nothing when service is disabled")
    void shouldDoNothingWhenDisabled() {
        DummyDataSource dummyDs = new DummyDataSource(true);
        KeepAliveService service = new KeepAliveService(dummyDs);
        service.setEnabled(false);

        service.pingKeepAlive();

        assertEquals(0, dummyDs.getConnectionCallCount());
    }

    @Test
    @DisplayName("warmupDatabase should validate database connection successfully")
    void shouldWarmupDatabaseSuccessfully() {
        DummyDataSource dummyDs = new DummyDataSource(true);
        KeepAliveService service = new KeepAliveService(dummyDs);

        boolean result = service.warmupDatabase();

        assertTrue(result);
        assertEquals(1, dummyDs.getConnectionCallCount());
    }

    @Test
    @DisplayName("warmupDatabase should return false gracefully on SQL exception")
    void shouldHandleDatabaseWarmupExceptionGracefully() {
        DummyDataSource dummyDs = new DummyDataSource(false, true);
        KeepAliveService service = new KeepAliveService(dummyDs);

        boolean result = service.warmupDatabase();

        assertFalse(result);
    }

    @Test
    @DisplayName("warmupDatabase should return false when DataSource is null")
    void shouldHandleNullDataSourceGracefully() {
        KeepAliveService service = new KeepAliveService(null);

        boolean result = service.warmupDatabase();

        assertFalse(result);
    }

    @Test
    @DisplayName("pingExternalUrl should return false gracefully when targetUrl is blank")
    void shouldHandleBlankTargetUrlGracefully() {
        KeepAliveService service = new KeepAliveService(null);
        service.setTargetUrl("   ");

        boolean result = service.pingExternalUrl();

        assertFalse(result);
    }

    @Test
    @DisplayName("pingExternalUrl should return false gracefully on connection error without crashing")
    void shouldHandleUnreachableHostGracefully() {
        KeepAliveService service = new KeepAliveService(null);
        service.setTargetUrl("http://127.0.0.1:59999/health");

        boolean result = service.pingExternalUrl();

        assertFalse(result);
    }

    /**
     * Minimal lightweight test stub for DataSource and Connection
     * avoiding Mockito bytecode manipulation on JDK interfaces.
     */
    private static class DummyDataSource implements DataSource {
        private final boolean valid;
        private final boolean throwOnConnect;
        private int connectionCallCount = 0;

        public DummyDataSource(boolean valid) {
            this(valid, false);
        }

        public DummyDataSource(boolean valid, boolean throwOnConnect) {
            this.valid = valid;
            this.throwOnConnect = throwOnConnect;
        }

        public int getConnectionCallCount() {
            return connectionCallCount;
        }

        @Override
        public Connection getConnection() throws SQLException {
            connectionCallCount++;
            if (throwOnConnect) {
                throw new SQLException("Simulated database connectivity outage");
            }
            return (Connection) java.lang.reflect.Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("isValid".equals(method.getName())) {
                            return valid;
                        }
                        if ("close".equals(method.getName())) {
                            return null;
                        }
                        return null;
                    }
            );
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getConnection();
        }

        @Override
        public PrintWriter getLogWriter() { return null; }
        @Override
        public void setLogWriter(PrintWriter out) {}
        @Override
        public void setLoginTimeout(int seconds) {}
        @Override
        public int getLoginTimeout() { return 0; }
        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }
        @Override
        public <T> T unwrap(Class<T> iface) { return null; }
        @Override
        public boolean isWrapperFor(Class<?> iface) { return false; }
    }
}

package com.starkgroup.vms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DatabaseConnectivityTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Should successfully connect to the PostgreSQL database and verify credentials and database name")
    void shouldConnectToDatabase() throws Exception {
        assertNotNull(dataSource, "DataSource should be configured and injected");

        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection, "Connection should be established");
            assertTrue(connection.isValid(2), "Connection should be valid");

            String productName = connection.getMetaData().getDatabaseProductName();
            assertEquals("PostgreSQL", productName, "Database product should be PostgreSQL");

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT current_database(), current_user")) {
                assertTrue(rs.next(), "Query should return at least one row");
                String currentDb = rs.getString(1);
                String currentUser = rs.getString(2);

                assertEquals("stark_vms_db", currentDb, "Database name should match stark_vms_db");
                assertEquals("vms_user", currentUser, "Database user should match vms_user");
            }
        }
    }
}

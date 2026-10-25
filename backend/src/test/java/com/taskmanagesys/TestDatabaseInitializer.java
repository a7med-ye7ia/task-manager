package com.taskmanagesys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDatabaseInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger log = LoggerFactory.getLogger(TestDatabaseInitializer.class);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        String host = applicationContext.getEnvironment().getProperty("DB_HOST", "localhost");
        String port = applicationContext.getEnvironment().getProperty("DB_PORT", "5432");
        String user = applicationContext.getEnvironment().getProperty("DB_USERNAME", "postgres");
        String pass = applicationContext.getEnvironment().getProperty("DB_PASSWORD", "postgres");
        String testDb = applicationContext.getEnvironment().getProperty("DB_TEST_NAME", "taskmanagesys_test");

        String maintenanceUrl = String.format("jdbc:postgresql://%s:%s/postgres", host, port);

        try (Connection conn = DriverManager.getConnection(maintenanceUrl, user, pass);
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery(
                    String.format("SELECT 1 FROM pg_database WHERE datname = '%s'", testDb)
            );
            if (!rs.next()) {
                log.info("Test database '{}' missing. Creating automatically...", testDb);
                stmt.execute(String.format("CREATE DATABASE \"%s\"", testDb));
                log.info("Test database '{}' created successfully.", testDb);
            }
        } catch (Exception e) {
            log.warn("Could not automatically verify/create test database '{}': {}", testDb, e.getMessage());
        }
    }
}

package com.csports.migration;

import org.flywaydb.core.Flyway;

public final class DatabaseMigration {

    public static void main(String[] args) {
        Flyway.configure()
                .dataSource(requiredEnvironment("DATABASE_URL"),
                        requiredEnvironment("DATABASE_USERNAME"),
                        requiredEnvironment("DATABASE_PASSWORD"))
                .locations("classpath:db/migration", "classpath:db/postgresql", "classpath:db/deployment")
                .failOnMissingLocations(true)
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .load()
                .migrate();
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }
}

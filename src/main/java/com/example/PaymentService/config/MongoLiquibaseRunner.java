package com.example.PaymentService.config;

import jakarta.annotation.PostConstruct;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.stereotype.Component;

@Component
public class MongoLiquibaseRunner {

    private static final String CHANGELOG = "db/master.yml";
    private static final String URL = System.getenv("MONGO_URL");

    @PostConstruct
    public void runMigrations() {
        try {
            Database database = DatabaseFactory.getInstance()
                    .openDatabase(URL, null, null, null, new ClassLoaderResourceAccessor());

            Liquibase liquibase = new Liquibase(
                    CHANGELOG,
                    new ClassLoaderResourceAccessor(),
                    database
            );

            liquibase.update((String)null);

            System.out.println("MongoDB migrations applied successfully!");

        } catch (Exception e) {
            throw new RuntimeException("Failed to run MongoDB Liquibase migrations", e);
        }
    }
}
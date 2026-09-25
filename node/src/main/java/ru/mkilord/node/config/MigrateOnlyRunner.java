package ru.mkilord.node.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * With {@code app.migrate-only=true} the node applies Liquibase migrations and exits.
 * Docker Compose runs it once before the replicas, so they do not race to create the schema.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty("app.migrate-only")
public class MigrateOnlyRunner implements CommandLineRunner {

    private final ConfigurableApplicationContext context;

    @Override
    public void run(String... args) {
        log.info("Migrations applied, exiting (app.migrate-only=true)");
        System.exit(SpringApplication.exit(context, () -> 0));
    }
}

package ru.mkilord.dispatcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Import;
import ru.mkilord.common.messaging.RabbitTopologyConfig;

@SpringBootApplication
@ConfigurationPropertiesScan
@Import(RabbitTopologyConfig.class)
public class DispatcherApplication {
    public static void main(String[] args) {
        SpringApplication.run(DispatcherApplication.class, args);
    }
}

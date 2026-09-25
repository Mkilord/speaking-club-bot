package ru.mkilord.node;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.mkilord.common.messaging.RabbitTopologyConfig;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
@Import(RabbitTopologyConfig.class)
public class NodeApplication {
    public static void main(String[] args) {
        SpringApplication.run(NodeApplication.class, args);
    }
}

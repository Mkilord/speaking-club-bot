package ru.mkilord.node.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class BotConfig {
    @Value("${bot.manager_id}")
    String adminId;
    @Value("${bot.context_limit}")
    int limit;
}

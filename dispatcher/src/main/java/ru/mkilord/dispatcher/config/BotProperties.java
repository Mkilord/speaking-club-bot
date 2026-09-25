package ru.mkilord.dispatcher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties("app.bot")
public record BotProperties(String token, String username) {
    public BotProperties {
        Assert.hasText(token, "BOT_TOKEN is not set");
        Assert.hasText(username, "BOT_USERNAME is not set");
    }
}

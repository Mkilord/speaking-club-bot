package ru.mkilord.node.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Set;

/**
 * @param moderatorIds Telegram ids that get the moderator role on registration
 * @param dialogTtl    an unfinished dialog is dropped after this idle time
 * @param zone         time zone of the club: meeting dates and times are entered in it
 */
@ConfigurationProperties("app.bot")
public record BotProperties(
        @DefaultValue Set<Long> moderatorIds,
        @DefaultValue("24h") Duration dialogTtl,
        @DefaultValue("Europe/Moscow") ZoneId zone) {
}

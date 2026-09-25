package ru.mkilord.common.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Queue names shared by dispatcher and node.
 *
 * @param updates Telegram updates: dispatcher -> node
 * @param answers outgoing messages: node -> dispatcher
 */
@ConfigurationProperties("app.rabbit.queues")
public record BotQueues(
        @DefaultValue("bot.updates") String updates,
        @DefaultValue("bot.answers") String answers) {

    public String deadLetter(String queue) {
        return queue + ".dlq";
    }
}

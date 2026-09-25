package ru.mkilord.common.messaging;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the same topology in both services, so it does not matter which one starts first.
 * <p>
 * Each work queue is durable and dead-letters rejected messages into its own DLQ.
 * Messages are published through the default exchange with the queue name as routing key.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(BotQueues.class)
public class RabbitTopologyConfig {

    public static final String DEAD_LETTER_EXCHANGE = "bot.dlx";

    @Bean
    public Declarables botTopology(BotQueues queues) {
        var dlx = new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);

        var updates = workQueue(queues.updates(), queues.deadLetter(queues.updates()));
        var updatesDlq = QueueBuilder.durable(queues.deadLetter(queues.updates())).build();

        var answers = workQueue(queues.answers(), queues.deadLetter(queues.answers()));
        var answersDlq = QueueBuilder.durable(queues.deadLetter(queues.answers())).build();

        return new Declarables(
                dlx,
                updates, updatesDlq, BindingBuilder.bind(updatesDlq).to(dlx).with(updatesDlq.getName()),
                answers, answersDlq, BindingBuilder.bind(answersDlq).to(dlx).with(answersDlq.getName()));
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    private static Queue workQueue(String name, String deadLetterRoutingKey) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(deadLetterRoutingKey)
                .build();
    }
}

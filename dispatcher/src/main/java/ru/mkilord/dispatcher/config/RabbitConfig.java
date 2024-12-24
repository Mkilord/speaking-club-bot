package ru.mkilord.dispatcher.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Value("${spring.rabbitmq.queues.text-message-update}")
    private String textMessageUpdateQueue;

    @Value("${spring.rabbitmq.queues.answer-message}")
    private String answerMessageQueue;

    @Bean
    public Queue messageQueue() {
        return new Queue(textMessageUpdateQueue);
    }

    @Bean
    public Queue answerQueue() {
        return new Queue(answerMessageQueue);
    }
}

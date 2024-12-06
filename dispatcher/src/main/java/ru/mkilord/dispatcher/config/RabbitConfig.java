package ru.mkilord.dispatcher.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static ru.mkilord.model.RabbitQueue.*;

@Configuration
public class RabbitConfig {
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue textMessageQueue() {
        return new Queue(TEXT_MESSAGE_UPDATE);
    }

    @Bean
    public Queue callBackQueryQueue() {
        return new Queue(CALLBACK_QUERY_UPDATE);
    }

    @Bean
    public Queue answerQueue() {
        return new Queue(ANSWER_MESSAGE);
    }
}

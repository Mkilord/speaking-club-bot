package ru.mkilord.node.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.mkilord.node.service.ProducerService;

import static lombok.AccessLevel.PRIVATE;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class ProducerServiceImpl implements ProducerService {
    RabbitTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.queues.answer-message}")
    @NonFinal
    private String answerMessageQueue;

    @Override
    public void produceAnswer(SendMessage message) {
        rabbitTemplate.convertAndSend(answerMessageQueue, message);
    }
}

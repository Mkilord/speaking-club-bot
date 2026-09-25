package ru.mkilord.node.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.mkilord.common.messaging.BotQueues;

@Component
@RequiredArgsConstructor
public class AnswerPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final BotQueues queues;

    public void publish(SendMessage message) {
        rabbitTemplate.convertAndSend(queues.answers(), message);
    }
}

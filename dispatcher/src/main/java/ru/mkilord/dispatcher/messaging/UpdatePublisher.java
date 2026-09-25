package ru.mkilord.dispatcher.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.mkilord.common.messaging.BotQueues;

@Component
@RequiredArgsConstructor
public class UpdatePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final BotQueues queues;

    public void publish(Update update) {
        rabbitTemplate.convertAndSend(queues.updates(), update);
    }
}

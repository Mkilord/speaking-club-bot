package ru.mkilord.dispatcher.service;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.mkilord.dispatcher.controller.UpdateController;

import static lombok.AccessLevel.PRIVATE;

@Service
@FieldDefaults(level = PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AnswerConsumerImpl implements AnswerConsumer {
    UpdateController updateController;

    @RabbitListener(queues = "${spring.rabbitmq.queues.answer-message}")
    @Override
    public void consume(SendMessage sendMessage) {
        updateController.sendAnswerMessage(sendMessage);
    }
}

package ru.mkilord.node.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.mkilord.node.service.ConsumerService;
import ru.mkilord.node.service.ProducerService;

import static lombok.AccessLevel.PRIVATE;
import static org.telegram.telegrambots.meta.api.methods.send.SendMessage.builder;
import static ru.mkilord.model.RabbitQueue.CALLBACK_QUERY_UPDATE;
import static ru.mkilord.model.RabbitQueue.TEXT_MESSAGE_UPDATE;

@Service
@Log4j2
@FieldDefaults(level = PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ConsumerServiceImpl implements ConsumerService {
    ProducerService updateProducerService;

    @Override
    @RabbitListener(queues = TEXT_MESSAGE_UPDATE)
    public void consumeTextMessageUpdates(Update update) {

        var chatId = update.getMessage().getChatId();
        var text = update.getMessage().getText();

        log.debug("Received text message with text: " + text);

        var outMsg = builder()
                .chatId(chatId)
                .text("Получено сообщение: " + text)
                .build();

        updateProducerService.produceAnswer(outMsg);
    }

    @Override
    @RabbitListener(queues = CALLBACK_QUERY_UPDATE)
    public void consumeCallbackQueryUpdates(Update update) {
        var callbackId = update.getCallbackQuery().getId();
        var data = update.getCallbackQuery().getData();

        log.debug("Press button with data: " + data);

        var outMsg = builder()
                .chatId(callbackId)
                .text("Нажата кнопка: " + data)
                .build();

        updateProducerService.produceAnswer(outMsg);
    }
}

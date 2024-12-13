package ru.mkilord.node.service.impl;

import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.mkilord.node.service.ConsumerService;
import ru.mkilord.node.service.NodeTelegramBot;

import static lombok.AccessLevel.PRIVATE;
import static ru.mkilord.model.RabbitQueue.MESSAGE_UPDATE;

@Service
@Log4j2
@FieldDefaults(level = PRIVATE, makeFinal = true)
@AllArgsConstructor
public class ConsumerServiceImpl implements ConsumerService {
    NodeTelegramBot telegramBot;

    @Override
    @RabbitListener(queues = MESSAGE_UPDATE)
    public void consumeMessageUpdates(Update update) {
        var text = "";

        if (update.hasMessage()) {
            text = update.getMessage().getText();
            log.debug("Received message with text: " + text);
            telegramBot.onMessageReceived(update);
        } else if (update.hasCallbackQuery()) {
            text = update.getCallbackQuery().getData();
            log.debug("Received message with button callback: " + text);
            telegramBot.onCallbackQueryReceived(update);
        }
    }
}

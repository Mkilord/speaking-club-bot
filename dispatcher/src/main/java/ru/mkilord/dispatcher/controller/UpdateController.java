package ru.mkilord.dispatcher.controller;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.mkilord.dispatcher.service.UpdateProducer;
import ru.mkilord.model.RabbitQueue;

import java.util.Objects;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class UpdateController {
    @NonFinal
    TelegramBot telegramBot;
    UpdateProducer producer;

    public void registerBot(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    public void processUpdate(Update update) {
        if (Objects.isNull(update)) {
            log.error("Received update is null");
            return;
        }
        if (update.hasMessage()) {
            distributeMessageByType(update);
            return;
        }
        if (update.hasCallbackQuery()) {
            processCallbackQuery(update);
            return;
        }
        log.error("Received unsupported message type " + update);
    }

    private void distributeMessageByType(Update update) {
        if (update.getMessage().hasText()) {
            processTextMessage(update);
            return;
        }
        var chatId = update.getMessage().getChatId();
        telegramBot.sendMsg(chatId, "Не поддерживаемый тип сообщения!");
    }

    private void processCallbackQuery(Update update) {
        producer.produce(RabbitQueue.CALLBACK_QUERY_UPDATE, update);
        telegramBot.answerCallbackQuery(update.getCallbackQuery().getId());
    }

    private void processTextMessage(Update update) {
        producer.produce(RabbitQueue.TEXT_MESSAGE_UPDATE, update);
    }
}

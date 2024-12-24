package ru.mkilord.dispatcher.controller;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.mkilord.dispatcher.config.RabbitConfig;
import ru.mkilord.dispatcher.service.UpdateProducer;

import java.util.Objects;

import static lombok.AccessLevel.PRIVATE;
import static org.telegram.telegrambots.meta.api.methods.send.SendMessage.builder;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class UpdateController {
    @NonFinal
    TelegramBot telegramBot;
    UpdateProducer producer;
    RabbitConfig rabbitConfig;

    public void registerBot(TelegramBot telegramBot) {
        log.debug("Register bot: " + telegramBot.getBotUsername());
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
        var outMsg = builder()
                .chatId(update.getMessage().getChatId())
                .text("Не поддерживаемый тип сообщения!")
                .build();
        log.error("Received unsupported message type " + update);

        telegramBot.sendMsg(outMsg);
    }

    private void processCallbackQuery(Update update) {
        var callbackQuery = update.getCallbackQuery();
        log.debug("processCallbackQuery: " + callbackQuery.getData());
        producer.produce(rabbitConfig.messageQueue().getName(), update);
        telegramBot.answerCallbackQuery(callbackQuery.getId());
    }

    private void processTextMessage(Update update) {
        log.debug("Process text message: " + update.getMessage().getText());
        producer.produce(rabbitConfig.messageQueue().getName(), update);
    }

    public void sendAnswerMessage(SendMessage sendMessage) {
        telegramBot.sendMsg(sendMessage);
    }
}

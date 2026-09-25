package ru.mkilord.dispatcher.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.mkilord.dispatcher.messaging.UpdatePublisher;

/**
 * Decides what to do with an incoming update.
 * Text messages and button clicks go to the node through RabbitMQ, everything else is answered here.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateRouter {

    static final String UNSUPPORTED_TEXT = "Бот понимает только текстовые сообщения и кнопки.";
    static final String UNAVAILABLE_TEXT = "Сервис временно недоступен, попробуйте чуть позже.";

    private final UpdatePublisher publisher;

    public void route(Update update, AbsSender sender) {
        if (update.hasCallbackQuery()) {
            // Telegram shows a spinner on the button until the callback is answered.
            execute(sender, AnswerCallbackQuery.builder()
                    .callbackQueryId(update.getCallbackQuery().getId())
                    .build());
            publish(update, update.getCallbackQuery().getMessage().getChatId(), sender);
            return;
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            publish(update, update.getMessage().getChatId(), sender);
            return;
        }
        if (update.hasMessage()) {
            execute(sender, new SendMessage(update.getMessage().getChatId().toString(), UNSUPPORTED_TEXT));
            return;
        }
        log.debug("Ignored update {} without message or callback", update.getUpdateId());
    }

    private void publish(Update update, long chatId, AbsSender sender) {
        try {
            publisher.publish(update);
        } catch (AmqpException e) {
            log.error("Could not publish update {}", update.getUpdateId(), e);
            execute(sender, new SendMessage(String.valueOf(chatId), UNAVAILABLE_TEXT));
        }
    }

    private void execute(AbsSender sender, AnswerCallbackQuery method) {
        try {
            sender.execute(method);
        } catch (TelegramApiException e) {
            log.warn("Could not answer callback query: {}", e.getMessage());
        }
    }

    private void execute(AbsSender sender, SendMessage method) {
        try {
            sender.execute(method);
        } catch (TelegramApiException e) {
            log.warn("Could not send message: {}", e.getMessage());
        }
    }
}

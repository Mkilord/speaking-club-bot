package ru.mkilord.dispatcher.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import ru.mkilord.dispatcher.bot.TelegramBot;

/**
 * Sends answers produced by the node.
 * <p>
 * Errors that make sense to retry (rate limit, network) are rethrown, so the listener
 * retries the message and finally moves it to the DLQ. Errors caused by the request itself
 * (the user blocked the bot, the chat does not exist) are logged and dropped.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnswerListener {

    private static final int TOO_MANY_REQUESTS = 429;

    private final TelegramBot bot;

    @RabbitListener(queues = "${app.rabbit.queues.answers:bot.answers}")
    public void onAnswer(SendMessage message) throws TelegramApiException {
        try {
            bot.execute(message);
        } catch (TelegramApiRequestException e) {
            if (isRetryable(e)) {
                throw e;
            }
            log.warn("Telegram rejected message to chat {}: {} {}", message.getChatId(), e.getErrorCode(), e.getApiResponse());
        }
    }

    public static boolean isRetryable(TelegramApiRequestException e) {
        var code = e.getErrorCode();
        return code == null || code == TOO_MANY_REQUESTS || code >= 500;
    }
}

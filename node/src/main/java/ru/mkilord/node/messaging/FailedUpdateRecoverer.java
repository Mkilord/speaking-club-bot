package ru.mkilord.node.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.mkilord.node.dialog.DialogService;
import ru.mkilord.node.util.UpdateUtils;

/**
 * Runs when an update failed all retries. Resets the user's dialog, apologizes
 * and rejects the message, so RabbitMQ moves it to the dead letter queue for analysis.
 * Spring Boot picks this bean up for the listener retry automatically.
 */
@Slf4j
@Component
public class FailedUpdateRecoverer extends RejectAndDontRequeueRecoverer {

    static final String APOLOGY = "Что-то пошло не так, и я не смог обработать сообщение. "
            + "Текущий диалог сброшен, попробуйте ещё раз: /help";

    private final ObjectMapper objectMapper;
    private final DialogService dialogService;
    private final AnswerPublisher answerPublisher;

    public FailedUpdateRecoverer(ObjectMapper objectMapper, DialogService dialogService, AnswerPublisher answerPublisher) {
        this.objectMapper = objectMapper;
        this.dialogService = dialogService;
        this.answerPublisher = answerPublisher;
    }

    @Override
    public void recover(Message message, Throwable cause) {
        log.error("Update moved to DLQ after retries", cause);
        try {
            var update = objectMapper.readValue(message.getBody(), Update.class);
            var chatId = UpdateUtils.chatId(update);
            dialogService.resetDialog(chatId);
            answerPublisher.publish(new SendMessage(String.valueOf(chatId), APOLOGY));
        } catch (Exception e) {
            log.warn("Could not notify the user about the failed update", e);
        }
        super.recover(message, cause);
    }
}

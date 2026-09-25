package ru.mkilord.node.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.mkilord.node.dialog.DialogService;

/**
 * Entry point of the node. Several replicas can listen to the same queue:
 * {@link DialogService} serializes updates of one chat with a row lock.
 * <p>
 * Answers are published after the transaction commits. If publishing fails, the update is
 * redelivered, recognized as processed and skipped, so the state is never applied twice.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateListener {

    private final DialogService dialogService;
    private final AnswerPublisher answerPublisher;

    @RabbitListener(queues = "${app.rabbit.queues.updates:bot.updates}")
    public void onUpdate(Update update) {
        log.debug("Update {} received", update.getUpdateId());
        var answers = dialogService.process(update);
        answers.forEach(answerPublisher::publish);
    }
}

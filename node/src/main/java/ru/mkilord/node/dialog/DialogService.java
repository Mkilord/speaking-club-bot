package ru.mkilord.node.dialog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.mkilord.node.config.BotProperties;
import ru.mkilord.node.fsm.DialogEngine;
import ru.mkilord.node.fsm.MessageContext;
import ru.mkilord.node.service.UserService;
import ru.mkilord.node.util.UpdateUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Processes one update in one transaction:
 * <ol>
 *     <li>skips the update if it was already applied (RabbitMQ delivers at least once);</li>
 *     <li>locks the dialog state of the chat (other replicas wait for this chat, not for others);</li>
 *     <li>runs the dialog engine and saves the new state.</li>
 * </ol>
 * Returns the messages to send. The caller publishes them after the commit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DialogService {

    private final DialogStateRepository stateRepository;
    private final UserService userService;
    private final DialogEngine engine;
    private final BotProperties properties;
    private final Clock clock;

    @Transactional
    public List<SendMessage> process(Update update) {
        var chatId = UpdateUtils.chatId(update);
        if (stateRepository.markProcessed(update.getUpdateId()) == 0) {
            log.info("Update {} of chat {} is already processed, skipping", update.getUpdateId(), chatId);
            return List.of();
        }
        stateRepository.createIfAbsent(chatId);
        var state = stateRepository.lockByChatId(chatId)
                .orElseThrow(() -> new IllegalStateException("Dialog state of chat " + chatId + " is missing"));

        var now = Instant.now(clock);
        state.resetIfIdleLongerThan(properties.dialogTtl(), now);

        var user = userService.syncFromTelegram(UpdateUtils.sender(update), chatId);
        var context = new MessageContext(update, user, state);
        engine.handle(context);

        state.touch(now);
        return context.getOutbox();
    }

    /** Called when an update failed all retries: the user should not stay stuck in a broken dialog. */
    @Transactional
    public void resetDialog(long chatId) {
        stateRepository.resetDialog(chatId);
    }
}

package ru.mkilord.node.dialog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.mkilord.node.config.BotProperties;

import java.time.Clock;
import java.time.Instant;

/** Removes states of chats that were idle for a long time. Safe to run on every replica. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DialogCleanupJob {

    private final DialogStateRepository repository;
    private final BotProperties properties;
    private final Clock clock;

    @Transactional
    @Scheduled(cron = "${app.bot.cleanup-cron:0 30 4 * * *}")
    public void deleteIdleStates() {
        var before = Instant.now(clock).minus(properties.dialogTtl());
        var states = repository.deleteIdleBefore(before);
        // Redeliveries come within minutes, a week of processed ids is more than enough.
        var updates = repository.deleteProcessedBefore(Instant.now(clock).minus(java.time.Duration.ofDays(7)));
        log.info("Cleanup: deleted {} idle dialog states and {} processed update ids", states, updates);
    }
}

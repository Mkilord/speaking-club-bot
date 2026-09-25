package ru.mkilord.node.dialog;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DialogStateTest {

    private static final Instant NOW = Instant.parse("2026-09-25T10:00:00Z");

    @Test
    void dropsDialogAfterIdleTimeout() {
        var state = new DialogState(1L);
        state.touch(NOW);
        state.setReplyId("/register1");
        state.getValues().put("firstName", "Иван");

        state.resetIfIdleLongerThan(Duration.ofHours(24), NOW.plus(Duration.ofHours(23)));
        assertThat(state.getReplyId()).isEqualTo("/register1");

        state.resetIfIdleLongerThan(Duration.ofHours(24), NOW.plus(Duration.ofHours(25)));
        assertThat(state.getReplyId()).isNull();
        assertThat(state.getValues()).isEmpty();
    }
}

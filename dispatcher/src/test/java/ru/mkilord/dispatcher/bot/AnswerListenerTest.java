package ru.mkilord.dispatcher.bot;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import ru.mkilord.dispatcher.messaging.AnswerListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnswerListenerTest {

    @Test
    void retriesOnlyRateLimitAndServerErrors() {
        assertThat(AnswerListener.isRetryable(error(429))).isTrue();
        assertThat(AnswerListener.isRetryable(error(502))).isTrue();
        assertThat(AnswerListener.isRetryable(error(403))).isFalse();
        assertThat(AnswerListener.isRetryable(error(400))).isFalse();
    }

    private static TelegramApiRequestException error(int code) {
        var exception = mock(TelegramApiRequestException.class);
        when(exception.getErrorCode()).thenReturn(code);
        return exception;
    }
}

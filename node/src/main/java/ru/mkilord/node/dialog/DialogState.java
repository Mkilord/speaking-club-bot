package ru.mkilord.node.dialog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Where a chat is in a dialog, stored in the database instead of node memory.
 * Any node replica can pick up the next message of the chat.
 */
@Entity
@Getter
@NoArgsConstructor
public class DialogState {

    @Id
    private Long chatId;

    /** Current step of the dialog, null when the chat is idle. */
    @Setter
    private String replyId;

    /** Values collected during the dialog. Only strings, so the state is always serializable. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dialog_values", nullable = false)
    private Map<String, String> values = new HashMap<>();

    @Column(nullable = false)
    private Instant updatedAt;

    public DialogState(Long chatId) {
        this.chatId = chatId;
        this.updatedAt = Instant.now();
    }

    public void touch(Instant now) {
        updatedAt = now;
    }

    public void resetIfIdleLongerThan(Duration ttl, Instant now) {
        if (updatedAt != null && updatedAt.plus(ttl).isBefore(now)) {
            reset();
        }
    }

    public void reset() {
        replyId = null;
        values.clear();
    }
}

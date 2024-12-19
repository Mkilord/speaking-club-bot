package ru.mkilord.node.common;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static lombok.AccessLevel.PRIVATE;

@Builder
@FieldDefaults(level = PRIVATE)
@Getter
@Setter
public class MessageContext {
    Update update;

    long chatId;
    String userRole;

    String currentReplyId;

    Map<String, String> values;

    public String getMessageText() {
        return update.getMessage().getText();
    }

    public void put(String key, String value) {
        if (values == null) values = new HashMap<>();
        values.put(key, value);
    }

    public String getValue(String key) {
        return values.get(key);
    }

    public void clear() {
        currentReplyId = null;
        if (Objects.nonNull(values)) values.clear();
    }

    public boolean hasReply() {
        return Objects.nonNull(currentReplyId);
    }
}

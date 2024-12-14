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

    String replyId;

    Map<String, String> values;

    public void put(String key, String value) {
        if (values == null) values = new HashMap<>();
        values.put(key, value);
    }

    public String getValues(String key) {
        return values.get(key);
    }

    public void clear() {
        replyId = null;
        if (values != null) values.clear();
    }

    public void clearValues() {
        values.clear();
    }

    public boolean hasReply() {
        return Objects.nonNull(replyId);
    }
}

package ru.mkilord.node.common;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.telegram.telegrambots.meta.api.objects.Update;

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
        values.put(key, value);
    }
    public String get(String key) {
        return values.get(key);
    }

    public void clear() {
        values.clear();
    }

    public boolean hasReply() {
        return Objects.nonNull(replyId);
    }
}

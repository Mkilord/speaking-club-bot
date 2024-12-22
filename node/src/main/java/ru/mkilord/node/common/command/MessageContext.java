package ru.mkilord.node.common.command;

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

    String replyId;

    Map<String, String> values;

    public String getText() {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getData();
        }
        return update.getMessage().getText();
    }

    public MessageContext setUpdate(Update update) {
        this.update = update;
        return this;
    }

    public void put(String key, String value) {
        if (Objects.isNull(values)) values = new HashMap<>();
        values.put(key, value);
    }

    public String getValue(String key) {
        if (Objects.isNull(values)) {
            throw new NullPointerException("Values are null");
        }
        return values.get(key);
    }

    public void clear() {
        replyId = null;
        if (Objects.nonNull(values)) values.clear();
    }

    public boolean hasReply() {
        return Objects.nonNull(replyId);
    }
}

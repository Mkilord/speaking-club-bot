package ru.mkilord.node.command;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.mkilord.node.command.menu.Menu;
import ru.mkilord.node.model.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static lombok.AccessLevel.PRIVATE;

@Builder
@FieldDefaults(level = PRIVATE)
@Data
public class MessageContext {
    Update update;

    String replyId;

    Menu menu;

    Map<String, Object> values;

    User user;

    public long getChatId() {
        return user.getChatId();
    }

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

    public <T> T getValue(String key, Class<T> clazz) {
        if (Objects.isNull(values)) throw new NullPointerException("Values are null");
        var value = values.get(key);
        if (Objects.isNull(value)) throw new NullPointerException("Value not found");
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        } else {
            throw new ClassCastException("Value is not of type " + clazz.getName());
        }
    }

    public String getValue(String key) {
        if (Objects.isNull(values)) {
            throw new NullPointerException("Values are null");
        }
        return (String) values.get(key);
    }

    public void clear() {
        replyId = null;
        menu = null;
        if (Objects.nonNull(values)) values.clear();
    }

    public boolean hasReply() {
        return Objects.nonNull(replyId);
    }
}

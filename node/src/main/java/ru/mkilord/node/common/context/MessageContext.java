package ru.mkilord.node.common.context;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.mkilord.node.common.menu.Menu;
import ru.mkilord.node.model.User;

import java.util.*;

import static lombok.AccessLevel.PRIVATE;

@Builder
@FieldDefaults(level = PRIVATE)
@Data
public class MessageContext {

    Update update;

    String replyId;

    Map<String, Object> values;

    User user;

    public Menu getMenu() {
        return getValue(Menu.MENU_KEY, Menu.class);
    }

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

    public void put(String key, Object value) {
        if (Objects.isNull(values)) values = new HashMap<>();
        values.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getValues(String key, Class<T> elementClass) {
        if (Objects.isNull(values)) throw new NullPointerException("Values are null");
        var value = values.get(key);
        if (!(value instanceof ArrayList<?> list)) {
            throw new ClassCastException("Value is not of type ArrayList");
        }
        for (Object item : list) {
            if (!elementClass.isInstance(item)) {
                throw new ClassCastException("Element is not of type " + elementClass.getName());
            }
        }
        return (List<T>) list;
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
        if (Objects.nonNull(values)) values.clear();
    }

    public boolean hasReply() {
        return Objects.nonNull(replyId);
    }
}

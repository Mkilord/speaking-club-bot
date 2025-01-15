package ru.mkilord.node.command.context;

import lombok.experimental.FieldDefaults;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static lombok.AccessLevel.PRIVATE;

@FieldDefaults(level = PRIVATE, makeFinal = true)
public class ContextCollection {

    float LOAD_FACTOR = 0.75f;

    Map<Long, MessageContext> contextMap;

    public ContextCollection(int limit) {
        this.contextMap = new LinkedHashMap<>(limit, LOAD_FACTOR, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, MessageContext> eldest) {
                return size() > limit;
            }
        };
    }

    public void remove(MessageContext context) {
        contextMap.remove(context.getChatId());
    }

    public void remove(Long chatId) {
        contextMap.remove(chatId);
    }

    public Optional<MessageContext> getContext(long key) {
        return Optional.ofNullable(contextMap.get(key));
    }

    public void addContext(long key, MessageContext context) {
        contextMap.put(key, context);
    }

}

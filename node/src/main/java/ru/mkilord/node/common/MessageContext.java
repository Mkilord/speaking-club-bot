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
    Reply reply;

    Map<String, String> values;

    public boolean hasReply() {
        return Objects.nonNull(reply);
    }
}

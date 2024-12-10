package ru.mkilord.node.common;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@Builder
@FieldDefaults(level = PRIVATE)
@Getter
public class MessageContext {
    Update update;
    long chatId;
    long userId;

    Map<String, String> values;
}

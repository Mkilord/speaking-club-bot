package ru.mkilord.node.common;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.HashMap;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@Setter
@Getter
@FieldDefaults(level = PRIVATE)
public class MessageContext {
    long chatId;
    long userId;

    Map<String, String> values = new HashMap<>();
}

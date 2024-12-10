package ru.mkilord.node.common;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.function.Consumer;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Reply {
    String name;
    Consumer<MessageContext> action;
}

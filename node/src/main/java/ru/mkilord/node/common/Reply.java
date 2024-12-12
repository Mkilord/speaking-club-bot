package ru.mkilord.node.common;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.Optional;
import java.util.function.Consumer;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Reply {
    String id;
    Consumer<MessageContext> action;
    Reply nextReplay;

    public Optional<Reply> getNextReplay() {
        return Optional.ofNullable(nextReplay);
    }

}

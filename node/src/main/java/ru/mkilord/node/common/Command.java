package ru.mkilord.node.common;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.function.Consumer;

import static lombok.AccessLevel.PRIVATE;

@FieldDefaults(level = PRIVATE)
@Setter
@Getter
public class Command {
    String name;
    Consumer<MessageContext> action;
    Reply reply;

    public void setReply(Reply reply) {
        reply.setName(name + "reply");
        this.reply = reply;
    }


}

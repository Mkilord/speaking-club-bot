package ru.mkilord.node.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@FieldDefaults(level = PRIVATE)
@Getter
@AllArgsConstructor(access = PRIVATE)
public class Command {
    String name;
    Consumer<MessageContext> action;
    Reply reply;

    public static Builder builder() {
        return new Builder();
    }

    @FieldDefaults(level = PRIVATE)
    public static class Builder {
        String name;
        Consumer<MessageContext> action;

        final List<Reply> replyList = new ArrayList<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder action(Consumer<MessageContext> action) {
            this.action = action;
            return this;
        }

        public Builder withReply(Consumer<MessageContext> replyAction) {
            var reply = new Reply();
            reply.setAction(replyAction);
            replyList.add(reply);
            return this;
        }
        public Command build() {
            if (replyList.isEmpty()) {
                return new Command(name, action, null);
            }

            var currentReply = replyList.getFirst();

            for (int i = 1; i < replyList.size(); i++) {
                var nextReply = replyList.get(i);
                currentReply.setNextReplay(nextReply);
                currentReply = nextReply;
            }
            return new Command(name, action, replyList.getFirst());
        }
    }
}

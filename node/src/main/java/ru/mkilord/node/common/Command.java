package ru.mkilord.node.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;

import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PRIVATE;

@Slf4j
@FieldDefaults(level = PRIVATE)
@Getter
@AllArgsConstructor(access = PACKAGE)
public class Command {
    String name;
    Function<MessageContext, Boolean> action;

    Reply reply;
    String role;
    String info;

    public Map<String, Reply> extractReplies() {
        var replyMap = new HashMap<String, Reply>();
        extractRepliesRecursive(reply, replyMap);
        return replyMap;
    }

    private void extractRepliesRecursive(Reply reply, Map<String, Reply> replyMap) {
        if (Objects.isNull(reply)) return;
        replyMap.put(reply.getId(), reply);
        reply.getNextReplay().ifPresent(nextReplay -> extractRepliesRecursive(nextReplay, replyMap));
    }

    public boolean matchConditions(MessageContext context) {
        var messageText = context.getUpdate().getMessage().getText();
        var role = context.getRole();
        var isCommandMatch = getName().equals(messageText);
        return isCommandMatch && this.role.equals(role);
    }

    public boolean processAction(MessageContext context) {
        var isContinue = getAction().apply(context);
        if (isContinue) {
            if (Objects.nonNull(reply)) {
                context.setReplyId(getReply().getId());
            }
            return true;
        }
        context.clear();
        return true;
    }


    public static Builder create() {
        return new Builder();
    }

    @FieldDefaults(level = PRIVATE)
    @Getter
    public static class Builder {
        String name;
        Function<MessageContext, Boolean> action;
        final List<Reply> replyList = new ArrayList<>();
        Role role = UserRole.USER;
        String info;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder access(Role role) {
            this.role = role;
            return this;
        }

        public Builder action(Function<MessageContext, Boolean> action) {
            this.action = action;
            return this;
        }

        public Builder info(String info) {
            this.info = info;
            return this;
        }

        public Builder reply(Function<MessageContext, Boolean> replyAction) {
            var reply = new Reply();
            reply.setAction(replyAction);
            reply.setId(name + replyList.size());
            replyList.add(reply);
            return this;
        }


        public Command build() {
            if (replyList.isEmpty()) {
                return new Command(name, action, null, role.get(), info);
            }
            var currentReply = replyList.getFirst();

            for (int i = 1; i < replyList.size(); i++) {
                var nextReply = replyList.get(i);
                currentReply.setNextReplay(nextReply);
                currentReply = nextReply;
            }
            return new Command(name, action, replyList.getFirst(), role.get(), info);
        }
    }
}

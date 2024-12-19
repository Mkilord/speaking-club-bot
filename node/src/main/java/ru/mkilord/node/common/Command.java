package ru.mkilord.node.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PRIVATE;

@Slf4j
@FieldDefaults(level = PRIVATE)
@Getter
@AllArgsConstructor(access = PACKAGE)
public class Command {

    String name;
    Function<MessageContext, Step> action;
    Reply reply;
    Set<String> roles;
    String info;

    public Optional<Reply> getReply() {
        return Optional.ofNullable(reply);
    }

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
        var role = context.getUserRole();
        var isCommandMatch = getName().equals(messageText);
        return isCommandMatch && roles.contains(role);
    }

    public boolean processAction(MessageContext context) {
        var nextStep = getAction().apply(context);
        if (nextStep == Step.NEXT) {
            getReply().ifPresentOrElse(reply -> context.setCurrentReplyId(reply.getId()), context::clear);
            return true;
        }
        if (nextStep == Step.TERMINATE) {
            context.clear();
            return true;
        }
        return false;
    }


    public static Builder create(String name) {
        return new Builder(name);
    }

    @FieldDefaults(level = PRIVATE)
    @Getter
    public static class Builder {
        final String name;
        Function<MessageContext, Step> action;
        final List<Reply> replyList = new ArrayList<>();
        Set<String> roles = new HashSet<>();
        String info;

        public Builder(String name) {
            this.name = name;
            roles.add(Role.USER.toString());
        }

        public Builder access(Role... roles) {
            this.roles = Arrays.stream(roles)
                    .map(Role::toString)
                    .collect(Collectors.toSet());
            return this;
        }

        public Builder action(Function<MessageContext, Step> action) {
            this.action = action;
            return this;
        }

        public Builder action(Consumer<MessageContext> action) {
            this.action = context -> {
                action.accept(context);
                return Step.NEXT;
            };
            return this;
        }

        public Builder info(String info) {
            this.info = info;
            return this;
        }

        private void createReply(Function<MessageContext, Step> action) {
            var reply = Reply.builder()
                    .id(name + replyList.size()).action(action)
                    .build();
            replyList.add(reply);
        }

        public Builder reply(Function<MessageContext, Step> replyAction) {
            createReply(replyAction);
            return this;
        }

        public Builder reply(Consumer<MessageContext> replyAction) {
            createReply(context -> {
                replyAction.accept(context);
                return Step.NEXT;
            });
            return this;
        }


        public Command build() {
            if (replyList.isEmpty()) {
                return new Command(name, action, null, roles, info);
            }
            var currentReply = replyList.getFirst();

            for (int i = 1; i < replyList.size(); i++) {
                var nextReply = replyList.get(i);
                currentReply.setNextReplay(nextReply);
                currentReply = nextReply;
            }
            return new Command(name, action, replyList.getFirst(), roles, info);
        }
    }
}

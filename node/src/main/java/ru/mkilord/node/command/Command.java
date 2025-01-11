package ru.mkilord.node.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import ru.mkilord.node.model.enums.Role;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@FieldDefaults(level = PRIVATE)
@Getter
@AllArgsConstructor(access = PRIVATE)
public class Command {

    String name;
    Function<MessageContext, Step> action;
    Reply reply;
    Set<String> roles;
    String info;

    public Optional<Reply> getReply() {
        return Optional.ofNullable(reply);
    }

    public Optional<Function<MessageContext, Step>> getNextStep() {
        return Optional.ofNullable(action);
    }

    public Map<String, Reply> extractReplies() {
        var replyMap = new HashMap<String, Reply>();
        extractRepliesRecursive(reply, replyMap);
        return replyMap;
    }

    private static void extractRepliesRecursive(Reply reply, Map<String, Reply> replyMap) {
        if (Objects.isNull(reply)) return;
        replyMap.put(reply.getId(), reply);
        reply.getNextReplay().ifPresent(nextReplay -> extractRepliesRecursive(nextReplay, replyMap));
    }

    public boolean matchConditions(MessageContext context) {
        var messageText = context.getText();
        var role = context.getUser().getRole();
        var isCommandMatch = getName().equals(messageText);
        return isCommandMatch && roles.contains(role.toString());
    }

    private void terminate(MessageContext context) {
        context.clear();
    }

    private Step tryGetNextStep(MessageContext context) {
        return getNextStep().map(action -> action.apply(context)).orElse(Step.NEXT);
    }

    private void tryGoToReply(MessageContext context) {
        getReply().ifPresentOrElse(reply -> {
            var nextStep = reply.getPreview().map(preview -> preview.apply(context));
            if (nextStep.isPresent() && nextStep.get() == Step.TERMINATE) {
                terminate(context);
                return;
            }
            context.setReplyId(reply.getId());
        }, () -> terminate(context));
    }

    public boolean processAction(MessageContext context) {
        var nextStep = tryGetNextStep(context);

        if (nextStep == Step.NEXT) {
            tryGoToReply(context);
            return true;
        }
        if (nextStep == Step.TERMINATE) {
            terminate(context);
            return true;
        }
        return false;
    }


    public static Builder create(String name) {
        return new Builder(name);
    }

    @FieldDefaults(level = PRIVATE)
    public static class Builder {
        final String name;
        Function<MessageContext, Step> action;
        final List<Reply> replyList = new ArrayList<>();
        Set<String> roles = new HashSet<>();
        String info;
        Consumer<MessageContext> post;

        public Builder(String name) {
            this.name = name;
            roles.add(Role.USER.toString());
        }

        public Builder access(Role... roles) {
            this.roles = Arrays.stream(roles).map(Role::toString).collect(Collectors.toSet());
            return this;
        }

        public Builder help(String info) {
            this.info = info;
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

        private void createReply(Reply.ReplyBuilder replyBuilder) {
            var reply = replyBuilder.build();
            reply.setId(name + replyList.size());
            replyList.add(reply);
        }

        public Builder input(Function<MessageContext, Step> inputAction) {
            var replyBuilder = Reply.builder().action(inputAction);
            createReply(replyBuilder);
            return this;
        }

        public Builder input(Reply.ReplyBuilder... replyBuilders) {
            Arrays.stream(replyBuilders).forEach(this::createReply);
            return this;
        }

        public Builder input(Reply.ReplyBuilder replyBuilder) {
            createReply(replyBuilder);
            return this;
        }

        public Builder input(Consumer<MessageContext> inputAction) {
            var replyBuilder = Reply.builder();
            replyBuilder.action(context -> {
                inputAction.accept(context);
                return Step.NEXT;
            });
            createReply(replyBuilder);
            return this;
        }

        /*Будет выполнен, после применения последнего Reply в очереди*/
        public Builder post(Consumer<MessageContext> postAction) {
            this.post = postAction;
            return this;
        }

        private void compileReplies() {
            var currentReply = replyList.getFirst();
            for (int i = 1; i < replyList.size(); i++) {
                var nextReply = replyList.get(i);
                currentReply.setNextReplay(nextReply);
                currentReply = nextReply;
            }
            replyList.getLast().setPost(post);
        }

        public Command build() {
            if (replyList.isEmpty()) {
                return new Command(name, action, null, roles, info);
            }
            compileReplies();
            return new Command(name, action, replyList.getFirst(), roles, info);
        }
    }
}

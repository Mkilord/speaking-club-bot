package ru.mkilord.node.command;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Reply {
    @Setter
    String id;

    final Function<MessageContext, Step> action;
    final Consumer<MessageContext> preview;

    @Setter
    Consumer<MessageContext> post;

    @Setter
    Reply nextReplay;

    public Optional<Consumer<MessageContext>> getPost() {
        return Optional.ofNullable(post);
    }

    public Optional<Function<MessageContext, Step>> getNextStep() {
        return Optional.ofNullable(action);
    }

    public Optional<Consumer<MessageContext>> getPreview() {
        return Optional.ofNullable(preview);
    }

    @Builder
    public Reply(Function<MessageContext, Step> action, Consumer<MessageContext> preview) {
        this.action = action;
        this.preview = preview;
    }

    private void tryGoToNextReply(MessageContext context) {
        getNextReplay().ifPresentOrElse(
                nextReply -> {
                    nextReply.getPreview().ifPresent(p -> p.accept(context));
                    context.setReplyId(nextReply.getId());
                }, () -> terminate(context));
    }

    private void showPost(MessageContext context) {
        getPost().ifPresent(post -> post.accept(context));
    }

    private Step tryGetNextStep(MessageContext context) {
        return getNextStep()
                .map(action -> action.apply(context))
                .orElse(Step.NEXT);
    }

    private void terminate(MessageContext context) {
        showPost(context);
        context.clear();
    }

    public boolean processAction(MessageContext context) {
        var nextStep = tryGetNextStep(context);
        if (nextStep == Step.NEXT) {
            tryGoToNextReply(context);
            return true;
        }
        if (nextStep == Step.TERMINATE) {
            terminate(context);
            return true;
        }
        return nextStep == Step.REPEAT;
    }

    public Optional<Reply> getNextReplay() {
        return Optional.ofNullable(nextReplay);
    }
}

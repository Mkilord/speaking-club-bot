package ru.mkilord.node.command;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.mkilord.node.command.context.MessageContext;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Reply {
    @Setter
    String id;

    final Function<MessageContext, Step> action;
    final Function<MessageContext, Step> preview;

    @Setter
    Consumer<MessageContext> post;

    @Setter
    Reply nextReplay;

    public Reply(Function<MessageContext, Step> action, Function<MessageContext, Step> preview) {
        this.action = action;
        this.preview = preview;
    }

    public static Reply.ReplyBuilder builder() {
        return new Reply.ReplyBuilder();
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

    public Optional<Consumer<MessageContext>> getPost() {
        return Optional.ofNullable(post);
    }

    public Optional<Function<MessageContext, Step>> getNextStep() {
        return Optional.ofNullable(action);
    }

    public Optional<Function<MessageContext, Step>> getPreview() {
        return Optional.ofNullable(preview);
    }

    public Optional<Reply> getNextReplay() {
        return Optional.ofNullable(nextReplay);
    }

    private void tryGoToNextReply(MessageContext context) {
        getNextReplay().ifPresentOrElse(
                nextReply -> {
                    var nextStep = nextReply.getPreview().map(preview -> preview.apply(context));
                    if (nextStep.isPresent() && nextStep.get() == Step.TERMINATE) {
                        terminate(context);
                        return;
                    }
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

    public static class ReplyBuilder {

        private Function<MessageContext, Step> action;
        private Function<MessageContext, Step> preview;

        public ReplyBuilder action(Function<MessageContext, Step> action) {
            this.action = action;
            return this;
        }

        public ReplyBuilder preview(Function<MessageContext, Step> preview) {
            this.preview = preview;
            return this;
        }

        public ReplyBuilder preview(Consumer<MessageContext> preview) {
            this.preview = context -> {
                preview.accept(context);
                return Step.NEXT;
            };
            return this;
        }

        public Reply build() {
            return new Reply(action, preview);
        }
    }

}

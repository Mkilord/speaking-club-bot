package ru.mkilord.node.common;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.Optional;
import java.util.function.Function;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Reply {
    String id;
    Function<MessageContext, Step> action;
    @Setter
    Reply nextReplay;

    public boolean processAction(MessageContext context) {
        var nextStep = getAction().apply(context);
        if (nextStep == Step.NEXT) {
            getNextReplay().ifPresentOrElse(nextReply -> context.setCurrentReplyId(nextReply.getId()), context::clear);
            return true;
        }
        if (nextStep == Step.REPEAT) {
            return true;
        }
        if (nextStep == Step.TERMINATE) {
            context.clear();
            return true;
        }
        return false;
    }

    public Optional<Reply> getNextReplay() {
        return Optional.ofNullable(nextReplay);
    }

}

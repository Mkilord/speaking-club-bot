package ru.mkilord.node.common;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.Optional;
import java.util.function.Function;

@Getter
//@Setter
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
        } else if (nextStep == Step.REPEAT) {
            return true;
        } else return nextStep == Step.TERMINATE;
    }

    public Optional<Reply> getNextReplay() {
        return Optional.ofNullable(nextReplay);
    }

}

package ru.mkilord.node.common;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.Optional;
import java.util.function.Function;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Reply {
    String id;
    Function<MessageContext, Boolean> action;
    Reply nextReplay;

    public boolean processAction(MessageContext context) {
        var isContinue = getAction().apply(context);
        if (isContinue) {
            getNextReplay().ifPresentOrElse(nextReply -> context.setReplyId(nextReply.getId()), context::clear);
        }
        return true;
    }

    public Optional<Reply> getNextReplay() {
        return Optional.ofNullable(nextReplay);
    }

}

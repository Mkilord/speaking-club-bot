package ru.mkilord.node.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.mkilord.node.fsm.MessageContext;
import ru.mkilord.node.model.Meet;
import ru.mkilord.node.model.User;
import ru.mkilord.node.util.MeetFormatter;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;

/** Messages to club subscribers and meeting participants. They go out together with the answer. */
@Component
@RequiredArgsConstructor
public class MeetNotifications {

    private final Clock clock;

    /** Tells club subscribers about a new meeting. Returns the number of recipients. */
    public int published(MessageContext context, Meet meet) {
        var text = "Клуб «%s» назначил встречу:\n\n%s\n\nЗаписаться: /clubs"
                .formatted(meet.getClub().getName(), MeetFormatter.format(meet, LocalDate.now(clock)));
        return send(context, meet.getClub().getSubscribers(), text);
    }

    /** Tells participants and subscribers that the meeting will not take place. */
    public int cancelled(MessageContext context, Meet meet) {
        var recipients = new LinkedHashSet<User>(meet.getParticipants());
        recipients.addAll(meet.getClub().getSubscribers());
        var text = "Встреча клуба «%s» отменена:\n\n%s"
                .formatted(meet.getClub().getName(), MeetFormatter.format(meet, LocalDate.now(clock)));
        return send(context, recipients, text);
    }

    private int send(MessageContext context, Collection<User> recipients, String text) {
        recipients.forEach(user -> context.sendTo(user.getChatId(), text));
        return recipients.size();
    }
}

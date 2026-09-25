package ru.mkilord.node.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.mkilord.node.fsm.Command;
import ru.mkilord.node.fsm.CommandCatalog;
import ru.mkilord.node.fsm.Input;
import ru.mkilord.node.fsm.Item;
import ru.mkilord.node.fsm.Menu;
import ru.mkilord.node.fsm.MessageContext;
import ru.mkilord.node.fsm.Step;
import ru.mkilord.node.model.Meet;
import ru.mkilord.node.model.MeetStatus;
import ru.mkilord.node.model.Role;
import ru.mkilord.node.model.User;
import ru.mkilord.node.service.ClubService;
import ru.mkilord.node.service.MeetNotifications;
import ru.mkilord.node.service.MeetService;
import ru.mkilord.node.util.MeetFormatter;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static ru.mkilord.node.command.Inputs.CLUB_ID;
import static ru.mkilord.node.command.Inputs.MEET_DATE;
import static ru.mkilord.node.command.Inputs.MEET_NAME;
import static ru.mkilord.node.command.Inputs.MEET_TIME;

/** Creating and running meetings. Available to organizers and moderators. */
@Component
@RequiredArgsConstructor
public class OrganizerCommands implements CommandCatalog {

    static final String MEET_ID = "meetId";
    private static final String MANAGE_HINT = "\n\nУправление встречами: /control_meeting";

    private final Inputs inputs;
    private final ClubService clubService;
    private final MeetService meetService;
    private final MeetNotifications notifications;
    private final Clock clock;

    @Override
    public List<Command> commands() {
        return List.of(createMeeting(), controlMeeting());
    }

    private Command createMeeting() {
        return Command.create("/create_meeting")
                .access(Role.EMPLOYEES)
                .help("создать встречу")
                .input(inputs.selectClub(), inputs.meetName(), inputs.meetDate(), inputs.meetTime())
                .input(Input.step(this::saveMeet), Input.menu(context -> Menu.builder("Опубликовать встречу сейчас?")
                        .item("publish", "📢 Да, опубликовать", this::publish)
                        .item("later", "Позже", ctx -> {
                            ctx.send("Встреча сохранена как скрытая." + MANAGE_HINT);
                            return Step.TERMINATE;
                        })
                        .build()))
                .build();
    }

    private Step saveMeet(MessageContext context) {
        var club = clubService.find(context.getLong(CLUB_ID));
        if (club.isEmpty()) {
            context.send("Клуб не найден, возможно, его удалили.");
            return Step.TERMINATE;
        }
        var meet = meetService.create(club.get(), context.get(MEET_NAME),
                LocalDate.parse(context.get(MEET_DATE)), LocalTime.parse(context.get(MEET_TIME)));
        context.put(MEET_ID, meet.getId());
        context.send("✅ Встреча создана:\n\n" + MeetFormatter.format(meet, LocalDate.now(clock)));
        return Step.NEXT;
    }

    private Command controlMeeting() {
        return Command.create("/control_meeting")
                .access(Role.EMPLOYEES)
                .help("управление встречами")
                .input(inputs.selectClub(), Input.menu(this::meetsMenu), Input.menu(this::meetActions))
                .build();
    }

    private Menu meetsMenu(MessageContext context) {
        var meets = meetService.findManageable(context.getLong(CLUB_ID));
        if (meets.isEmpty()) {
            context.send("У клуба нет активных встреч. Создать: /create_meeting");
            return null;
        }
        var today = LocalDate.now(clock);
        return Menu.builder("Выберите встречу:")
                .items(meets.stream()
                        .map(meet -> Item.of(meet.getId().toString(), statusIcon(meet) + " " + MeetFormatter.button(meet, today)))
                        .toList())
                .onSelect((ctx, key) -> {
                    ctx.put(MEET_ID, key);
                    return Step.NEXT;
                })
                .build();
    }

    private Menu meetActions(MessageContext context) {
        var meet = meetService.find(context.getLong(MEET_ID)).orElse(null);
        if (meet == null) {
            context.send("Встреча не найдена.");
            return null;
        }
        var menu = Menu.builder("Что сделать со встречей?\n\n" + MeetFormatter.formatWithStatus(meet, LocalDate.now(clock)));
        if (meet.getStatus() == MeetStatus.HIDDEN) {
            menu.item("publish", "📢 Опубликовать", this::publish);
            menu.item("delete", "🗑 Удалить", ctx -> {
                ctx.send(meetService.delete(meet.getId()) ? "Встреча удалена." + MANAGE_HINT : "Не удалось удалить встречу.");
                return Step.TERMINATE;
            });
        }
        if (meet.getStatus() == MeetStatus.PUBLISHED) {
            menu.item("participants", "👥 Участники", ctx -> {
                ctx.send(participants(meet));
                return Step.TERMINATE;
            });
            menu.item("complete", "✅ Отметить проведённой", ctx -> {
                ctx.send(meetService.complete(meet.getId()).isPresent()
                        ? "Встреча отмечена как проведённая." + MANAGE_HINT
                        : "Статус встречи уже изменился.");
                return Step.TERMINATE;
            });
            menu.item("cancel", "❌ Отменить", ctx -> {
                meetService.cancel(meet.getId()).ifPresentOrElse(
                        cancelled -> ctx.send("Встреча отменена, уведомлений отправлено: %d.%s"
                                .formatted(notifications.cancelled(ctx, cancelled), MANAGE_HINT)),
                        () -> ctx.send("Статус встречи уже изменился."));
                return Step.TERMINATE;
            });
        }
        return menu.build();
    }

    private Step publish(MessageContext context) {
        meetService.publish(context.getLong(MEET_ID)).ifPresentOrElse(
                meet -> context.send("📢 Встреча опубликована, уведомлений отправлено: %d.%s"
                        .formatted(notifications.published(context, meet), MANAGE_HINT)),
                () -> context.send("Встречу уже нельзя опубликовать."));
        return Step.TERMINATE;
    }

    private static String participants(Meet meet) {
        if (meet.getParticipants().isEmpty()) {
            return "На встречу пока никто не записался.";
        }
        var list = meet.getParticipants().stream()
                .sorted(Comparator.comparing(User::getFullName))
                .map(user -> "- " + user.getFullName())
                .collect(Collectors.joining("\n"));
        return "Участники (%d):\n%s".formatted(meet.getParticipants().size(), list);
    }

    private static String statusIcon(Meet meet) {
        return meet.getStatus() == MeetStatus.HIDDEN ? "🙈" : "📢";
    }
}

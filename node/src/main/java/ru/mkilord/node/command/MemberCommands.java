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
import ru.mkilord.node.model.Club;
import ru.mkilord.node.model.Role;
import ru.mkilord.node.service.ClubService;
import ru.mkilord.node.service.MeetService;
import ru.mkilord.node.util.MeetFormatter;
import ru.mkilord.node.util.Validators;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static ru.mkilord.node.command.Inputs.CLUB_ID;

/** Clubs, sign-ups and ratings for registered users. */
@Component
@RequiredArgsConstructor
public class MemberCommands implements CommandCatalog {

    static final String MEET_ID = "meetId";
    static final String RATING = "rating";

    private final Inputs inputs;
    private final ClubService clubService;
    private final MeetService meetService;
    private final Clock clock;

    @Override
    public List<Command> commands() {
        return List.of(clubs(), meets(), feedback());
    }

    private Command clubs() {
        return Command.create("/clubs")
                .access(Role.REGISTERED)
                .help("клубы: запись на встречу, подписка, описание")
                .input(inputs.selectClub(), clubActions(), openMeets())
                .build();
    }

    private Input clubActions() {
        return Input.menu(context -> club(context).map(club -> {
            var subscribed = clubService.isSubscribed(club.getId(), context.getUser());
            var menu = Menu.builder("Клуб «%s»".formatted(club.getName()))
                    .item("sign_up", "📅 Записаться на встречу", ctx -> Step.NEXT);
            if (subscribed) {
                menu.item("unsubscribe", "🔕 Отписаться от уведомлений", ctx -> {
                    clubService.unsubscribe(club.getId(), ctx.getUser());
                    ctx.send("🔕 Вы отписались от уведомлений клуба «%s».".formatted(club.getName()));
                    return Step.TERMINATE;
                });
            } else {
                menu.item("subscribe", "🔔 Подписаться на уведомления", ctx -> {
                    clubService.subscribe(club.getId(), ctx.getUser());
                    ctx.send("🔔 Готово! Пришлю сообщение, когда клуб «%s» назначит встречу.".formatted(club.getName()));
                    return Step.TERMINATE;
                });
            }
            return menu.item("about", "ℹ️ О клубе", ctx -> {
                ctx.send("ℹ️ %s\n\n%s\n\nРейтинг: %s".formatted(
                        club.getName(), club.getDescription(), clubService.rating(club.getId()).format()));
                return Step.TERMINATE;
            }).build();
        }).orElse(null));
    }

    private Input openMeets() {
        return Input.menu(context -> club(context).map(club -> {
            var meets = meetService.findOpen(club.getId());
            if (meets.isEmpty()) {
                context.send("У клуба «%s» пока нет открытых встреч.".formatted(club.getName()));
                return null;
            }
            var today = LocalDate.now(clock);
            return Menu.builder("Встречи клуба «%s»:".formatted(club.getName()))
                    .items(meets.stream()
                            .map(meet -> Item.of(meet.getId().toString(), MeetFormatter.button(meet, today)))
                            .toList())
                    .onSelect((ctx, key) -> {
                        var text = switch (meetService.signUp(Long.parseLong(key), ctx.getUser())) {
                            case SIGNED_UP -> "✔️ Записал вас на встречу. Ваши встречи: /meets";
                            case ALREADY_SIGNED_UP -> "Вы уже записаны на эту встречу. Ваши встречи: /meets";
                            case NOT_AVAILABLE -> "На эту встречу больше нельзя записаться.";
                        };
                        ctx.send(text);
                        return Step.TERMINATE;
                    })
                    .build();
        }).orElse(null));
    }

    private Command meets() {
        return Command.create("/meets")
                .access(Role.REGISTERED)
                .help("встречи, на которые вы записаны")
                .input(Input.menu(context -> {
                    var meets = meetService.findUpcomingFor(context.getUser());
                    if (meets.isEmpty()) {
                        context.send("Вы пока никуда не записаны. Выбрать встречу: /clubs");
                        return null;
                    }
                    var today = LocalDate.now(clock);
                    return Menu.builder("Ваши встречи:")
                            .items(meets.stream()
                                    .map(meet -> Item.of(meet.getId().toString(), MeetFormatter.button(meet, today)))
                                    .toList())
                            .onSelect((ctx, key) -> {
                                ctx.put(MEET_ID, key);
                                return Step.NEXT;
                            })
                            .build();
                }), Input.menu(context -> meetService.find(context.getLong(MEET_ID))
                        .map(meet -> Menu.builder(MeetFormatter.format(meet, LocalDate.now(clock)))
                                .item("cancel", "Отменить запись", ctx -> {
                                    meetService.cancelSignUp(meet.getId(), ctx.getUser());
                                    ctx.send("Запись отменена. Ваши встречи: /meets");
                                    return Step.TERMINATE;
                                })
                                .build())
                        .orElseGet(() -> {
                            context.send("Встреча не найдена.");
                            return null;
                        })))
                .build();
    }

    private Command feedback() {
        return Command.create("/feedback")
                .access(Role.REGISTERED)
                .help("оценить клуб")
                .input(inputs.selectClub(), Input.text(
                        "Оцените клуб от %d до %d.".formatted(ClubService.MIN_RATING, ClubService.MAX_RATING),
                        context -> {
                            var rating = Validators.parseIntInRange(context.getText(), ClubService.MIN_RATING, ClubService.MAX_RATING);
                            if (rating.isEmpty()) {
                                return Inputs.repeat(context, "Нужно целое число от %d до %d."
                                        .formatted(ClubService.MIN_RATING, ClubService.MAX_RATING));
                            }
                            context.put(RATING, rating.getAsInt());
                            return Step.NEXT;
                        }))
                .post(context -> {
                    var clubId = context.getLong(CLUB_ID);
                    if (clubService.find(clubId).isEmpty()) {
                        context.send("Клуб не найден.");
                        return;
                    }
                    clubService.rate(clubId, context.getUser(), Integer.parseInt(context.get(RATING)));
                    context.send("Спасибо, оценка сохранена! Текущий рейтинг: " + clubService.rating(clubId).format());
                })
                .build();
    }

    private Optional<Club> club(MessageContext context) {
        var club = clubService.find(context.getLong(CLUB_ID));
        if (club.isEmpty()) {
            context.send("Клуб не найден, возможно, его удалили.");
        }
        return club;
    }
}

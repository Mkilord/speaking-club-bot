package ru.mkilord.node.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.mkilord.node.fsm.Input;
import ru.mkilord.node.fsm.Item;
import ru.mkilord.node.fsm.Menu;
import ru.mkilord.node.fsm.MessageContext;
import ru.mkilord.node.fsm.Step;
import ru.mkilord.node.service.ClubService;
import ru.mkilord.node.service.UserService.Profile;
import ru.mkilord.node.util.Validators;
import ru.mkilord.node.util.Validators.FullName;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Dialog steps used by several commands. Values are stored in the dialog under the keys below. */
@Component
@RequiredArgsConstructor
public class Inputs {

    static final String LAST_NAME = "lastName";
    static final String FIRST_NAME = "firstName";
    static final String MIDDLE_NAME = "middleName";
    static final String PHONE = "phone";
    static final String EMAIL = "email";
    static final String CLUB_ID = "clubId";
    static final String CLUB_NAME = "clubName";
    static final String CLUB_DESCRIPTION = "clubDescription";
    static final String MEET_NAME = "meetName";
    static final String MEET_DATE = "meetDate";
    static final String MEET_TIME = "meetTime";

    static final int MEET_NAME_MIN = 5;
    static final int MEET_NAME_MAX = 250;
    static final int MAX_MONTHS_AHEAD = 1;
    static final LocalTime EARLIEST_MEET = LocalTime.of(8, 0);
    static final LocalTime LATEST_MEET = LocalTime.of(22, 0);

    private final ClubService clubService;
    private final Clock clock;

    Input fullName() {
        return Input.text("Введите ФИО через пробел, например: Иванов Иван Иванович. Отчество можно не указывать.",
                context -> Validators.parseFullName(context.getText())
                        .map(name -> {
                            context.put(LAST_NAME, name.lastName());
                            context.put(FIRST_NAME, name.firstName());
                            if (name.middleName() != null) {
                                context.put(MIDDLE_NAME, name.middleName());
                            }
                            return Step.NEXT;
                        })
                        .orElseGet(() -> repeat(context, "Не похоже на ФИО. Нужны фамилия и имя (и отчество, если есть), только буквы.")));
    }

    Input phone() {
        return Input.text("Введите номер телефона, например: +79106790783.",
                context -> Validators.normalizePhone(context.getText())
                        .map(phone -> {
                            context.put(PHONE, phone);
                            return Step.NEXT;
                        })
                        .orElseGet(() -> repeat(context, "Номер должен содержать от 10 до 15 цифр, например: +79106790783.")));
    }

    Input email() {
        return Input.text("Введите email.", context -> {
            var email = context.getText();
            if (!Validators.isEmail(email)) {
                return repeat(context, "Неверный формат email, попробуйте ещё раз.");
            }
            context.put(EMAIL, email);
            return Step.NEXT;
        });
    }

    static Profile profile(MessageContext context) {
        return new Profile(
                new FullName(context.get(LAST_NAME), context.get(FIRST_NAME), context.getOrNull(MIDDLE_NAME)),
                context.get(PHONE),
                context.get(EMAIL));
    }

    Input clubName() {
        return Input.text("Введите название клуба (от 2 до 50 символов).", context -> {
            var name = context.getText();
            if (!Validators.hasLength(name, 2, 50)) {
                return repeat(context, "Название должно быть от 2 до 50 символов.");
            }
            context.put(CLUB_NAME, name);
            return Step.NEXT;
        });
    }

    Input clubDescription() {
        return Input.text("Введите описание клуба (от 20 до 250 символов).", context -> {
            var description = context.getText();
            if (!Validators.hasLength(description, 20, 250)) {
                return repeat(context, "Описание должно быть от 20 до 250 символов.");
            }
            context.put(CLUB_DESCRIPTION, description);
            return Step.NEXT;
        });
    }

    /** Menu of all clubs. The chosen id is stored under {@link #CLUB_ID}. */
    Input selectClub() {
        return Input.menu(context -> {
            var clubs = clubService.findAll();
            if (clubs.isEmpty()) {
                context.send("Клубов пока нет.");
                return null;
            }
            return Menu.builder("Выберите клуб:")
                    .items(clubs.stream().map(club -> Item.of(club.getId().toString(), club.getName())).toList())
                    .onSelect((ctx, key) -> {
                        ctx.put(CLUB_ID, key);
                        return Step.NEXT;
                    })
                    .build();
        });
    }

    Input meetName() {
        return Input.text("Введите тему встречи.", context -> {
            var name = context.getText();
            if (!Validators.hasLength(name, MEET_NAME_MIN, MEET_NAME_MAX)) {
                return repeat(context, "Тема должна быть от %d до %d символов.".formatted(MEET_NAME_MIN, MEET_NAME_MAX));
            }
            context.put(MEET_NAME, name);
            return Step.NEXT;
        });
    }

    Input meetDate() {
        return Input.text("Введите дату встречи в формате ДД.ММ.ГГГГ, например: 05.10.2026.", context -> {
            var date = Validators.parseDate(context.getText());
            if (date.isEmpty()) {
                return repeat(context, "Не понял дату. Формат: ДД.ММ.ГГГГ, например: 05.10.2026.");
            }
            var today = LocalDate.now(clock);
            if (date.get().isBefore(today) || date.get().isAfter(today.plusMonths(MAX_MONTHS_AHEAD))) {
                return repeat(context, "Дата должна быть не раньше сегодняшней и не позже чем через месяц.");
            }
            context.put(MEET_DATE, date.get());
            return Step.NEXT;
        });
    }

    Input meetTime() {
        return Input.text("Введите время встречи в формате ЧЧ:ММ, например: 18:30.", context -> {
            var time = Validators.parseTime(context.getText());
            if (time.isEmpty()) {
                return repeat(context, "Не понял время. Формат: ЧЧ:ММ, например: 9:30 или 18:00.");
            }
            if (time.get().isBefore(EARLIEST_MEET) || time.get().isAfter(LATEST_MEET)) {
                return repeat(context, "Встреча может начинаться с 8:00 до 22:00.");
            }
            var date = LocalDate.parse(context.get(MEET_DATE));
            if (!LocalDateTime.of(date, time.get()).isAfter(LocalDateTime.now(clock))) {
                return repeat(context, "Это время уже прошло, введите другое.");
            }
            context.put(MEET_TIME, time.get());
            return Step.NEXT;
        });
    }

    static Step repeat(MessageContext context, String hint) {
        context.send(hint);
        return Step.REPEAT;
    }
}

package ru.mkilord.node.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.mkilord.node.fsm.Command;
import ru.mkilord.node.fsm.CommandCatalog;
import ru.mkilord.node.fsm.Input;
import ru.mkilord.node.fsm.Item;
import ru.mkilord.node.fsm.Menu;
import ru.mkilord.node.fsm.Step;
import ru.mkilord.node.model.Role;
import ru.mkilord.node.service.ClubService;
import ru.mkilord.node.service.UserService;

import java.util.List;

import static ru.mkilord.node.command.Inputs.CLUB_DESCRIPTION;
import static ru.mkilord.node.command.Inputs.CLUB_ID;
import static ru.mkilord.node.command.Inputs.CLUB_NAME;

/** Clubs and employees. Available to moderators only. */
@Component
@RequiredArgsConstructor
public class ModeratorCommands implements CommandCatalog {

    static final String EMPLOYEE_ID = "employeeId";

    private final Inputs inputs;
    private final ClubService clubService;
    private final UserService userService;

    @Override
    public List<Command> commands() {
        return List.of(createClub(), controlClubs(), addEmployee(), controlEmployees());
    }

    private Command createClub() {
        return Command.create("/create_club")
                .access(Role.MODERATOR)
                .help("создать клуб")
                .input(inputs.clubName(), inputs.clubDescription())
                .post(context -> {
                    var club = clubService.create(context.get(CLUB_NAME), context.get(CLUB_DESCRIPTION));
                    context.send("✅ Клуб «%s» создан. Управление клубами: /control_clubs".formatted(club.getName()));
                })
                .build();
    }

    private Command controlClubs() {
        return Command.create("/control_clubs")
                .access(Role.MODERATOR)
                .help("изменить или удалить клуб")
                .input(inputs.selectClub(), Input.menu(context -> clubService.find(context.getLong(CLUB_ID))
                        .map(club -> Menu.builder("Клуб «%s»".formatted(club.getName()))
                                .item("edit", "✏️ Изменить", ctx -> Step.NEXT)
                                .item("delete", "🗑 Удалить вместе со встречами", ctx -> {
                                    ctx.send(clubService.delete(club.getId()) ? "Клуб удалён." : "Клуб не найден.");
                                    return Step.TERMINATE;
                                })
                                .build())
                        .orElseGet(() -> {
                            context.send("Клуб не найден.");
                            return null;
                        })))
                .input(inputs.clubName(), inputs.clubDescription())
                .post(context -> clubService.update(context.getLong(CLUB_ID), context.get(CLUB_NAME), context.get(CLUB_DESCRIPTION))
                        .ifPresentOrElse(club -> context.send("✅ Клуб «%s» изменён.".formatted(club.getName())),
                                () -> context.send("Клуб не найден.")))
                .build();
    }

    private Command addEmployee() {
        return Command.create("/add_employee")
                .access(Role.MODERATOR)
                .help("назначить организатора")
                .input(Input.text("Введите Telegram-username пользователя, например: @ivanov. "
                        + "Он должен быть зарегистрирован в боте.", context -> {
                    var user = userService.findByUsername(context.getText());
                    if (user.isEmpty()) {
                        return Inputs.repeat(context, "Пользователь не найден. Проверьте username или начните заново: /help");
                    }
                    var employee = user.get();
                    switch (employee.getRole()) {
                        case USER -> context.send("Пользователь ещё не прошёл регистрацию (/register).");
                        case ORGANIZER, MODERATOR -> context.send("Пользователь уже сотрудник.");
                        case MEMBER -> {
                            userService.changeRole(employee, Role.ORGANIZER);
                            context.send("✅ %s теперь организатор.".formatted(employee.getFullName()));
                            context.sendTo(employee.getChatId(), "Вас назначили организатором встреч. Новые команды: /help");
                        }
                    }
                    return Step.TERMINATE;
                }))
                .build();
    }

    private Command controlEmployees() {
        return Command.create("/control_employees")
                .access(Role.MODERATOR)
                .help("организаторы")
                .input(Input.menu(context -> {
                    var employees = userService.findByRole(Role.ORGANIZER);
                    if (employees.isEmpty()) {
                        context.send("Организаторов пока нет. Назначить: /add_employee");
                        return null;
                    }
                    return Menu.builder("Выберите организатора:")
                            .items(employees.stream()
                                    .map(user -> Item.of(user.getTelegramId().toString(), user.getFullName()))
                                    .toList())
                            .onSelect((ctx, key) -> {
                                ctx.put(EMPLOYEE_ID, key);
                                return Step.NEXT;
                            })
                            .build();
                }), Input.menu(context -> userService.findById(context.getLong(EMPLOYEE_ID))
                        .filter(user -> user.getRole() == Role.ORGANIZER)
                        .map(employee -> Menu.builder(employee.getFullName())
                                .item("profile", "📛 Профиль", ctx -> {
                                    ctx.send("Данные организатора:\n\n" + ProfileCommands.describe(employee));
                                    return Step.TERMINATE;
                                })
                                .item("demote", "⬇️ Снять с должности", ctx -> {
                                    userService.changeRole(employee, Role.MEMBER);
                                    ctx.send("%s больше не организатор.".formatted(employee.getFullName()));
                                    ctx.sendTo(employee.getChatId(), "Вы больше не организатор встреч. Команды: /help");
                                    return Step.TERMINATE;
                                })
                                .build())
                        .orElseGet(() -> {
                            context.send("Организатор не найден.");
                            return null;
                        })))
                .build();
    }
}

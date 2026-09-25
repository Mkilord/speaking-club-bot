package ru.mkilord.node.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.mkilord.node.fsm.Command;
import ru.mkilord.node.fsm.CommandCatalog;
import ru.mkilord.node.fsm.Item;
import ru.mkilord.node.fsm.Menu;
import ru.mkilord.node.model.Role;
import ru.mkilord.node.service.UserService;

import java.util.List;

/** Commands for everyone who just opened the bot. */
@Component
@RequiredArgsConstructor
public class StartCommands implements CommandCatalog {

    private final Inputs inputs;
    private final UserService userService;

    @Override
    public List<Command> commands() {
        return List.of(start(), register());
    }

    private Command start() {
        return Command.create("/start")
                .access(Role.ALL)
                .action(context -> {
                    if (context.getUser().getRole() != Role.USER) {
                        context.send("С возвращением! Список команд: /help");
                        return;
                    }
                    context.send("""
                            👋 Привет! Это бот разговорных клубов.

                            Здесь можно:
                            - 📅 записываться на встречи клубов;
                            - 🔔 подписаться на уведомления о новых встречах;
                            - ✍️ оценить клуб.

                            Для начала зарегистрируйтесь: /register""");
                })
                .build();
    }

    private Command register() {
        return Command.create("/register")
                .access(Role.USER)
                .help("регистрация")
                .action(context -> context.send("Давайте познакомимся! 😊"))
                .input(inputs.fullName(), inputs.phone(), inputs.email())
                .post(context -> {
                    var user = context.getUser();
                    userService.register(user, Inputs.profile(context));
                    context.send("✅ Готово!\n\n" + ProfileCommands.describe(user));
                    context.send("Посмотреть, что умеет бот:", Menu.commands(Item.command("/help", "📘 Команды")));
                })
                .build();
    }
}

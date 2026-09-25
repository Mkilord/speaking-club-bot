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
import ru.mkilord.node.model.User;
import ru.mkilord.node.service.UserService;

import java.util.List;

/** Personal data of a registered user. */
@Component
@RequiredArgsConstructor
public class ProfileCommands implements CommandCatalog {

    private final Inputs inputs;
    private final UserService userService;

    @Override
    public List<Command> commands() {
        return List.of(profile(), editProfile(), deleteAccount());
    }

    static String describe(User user) {
        return """
                📛 ФИО: %s
                📱 Телефон: %s
                📧 Email: %s""".formatted(user.getFullName(), user.getPhone(), user.getEmail());
    }

    private Command profile() {
        return Command.create("/profile")
                .access(Role.REGISTERED)
                .help("ваш профиль")
                .action(context -> context.send("Ваши данные:\n\n" + describe(context.getUser()),
                        Menu.commands(Item.command("/edit_profile", "✏️ Изменить"))))
                .build();
    }

    private Command editProfile() {
        return Command.create("/edit_profile")
                .access(Role.REGISTERED)
                .help("изменить профиль")
                .input(inputs.fullName(), inputs.phone(), inputs.email())
                .post(context -> {
                    userService.updateProfile(context.getUser(), Inputs.profile(context));
                    context.send("✅ Данные обновлены.\n\n" + describe(context.getUser()));
                })
                .build();
    }

    private Command deleteAccount() {
        return Command.create("/delete_account")
                .access(Role.REGISTERED)
                .help("удалить свои данные")
                .input(Input.menu(context -> Menu.builder("🚮 Удалить все ваши данные? Это нельзя отменить.")
                        .item("yes", "Да, удалить", ctx -> {
                            userService.delete(ctx.getUser());
                            ctx.send("Ваши данные удалены. 👋 Чтобы начать заново, отправьте /start");
                            return Step.TERMINATE;
                        })
                        .item("no", "Нет", ctx -> {
                            ctx.send("Ничего не удалено 😊");
                            return Step.TERMINATE;
                        })
                        .build()))
                .build();
    }
}

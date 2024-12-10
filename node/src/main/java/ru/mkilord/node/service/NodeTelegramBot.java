package ru.mkilord.node.service;

import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.mkilord.node.common.Command;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static lombok.AccessLevel.PRIVATE;


@FieldDefaults(level = PRIVATE, makeFinal = true)
@Slf4j
@Component
public class NodeTelegramBot {

    ArrayList<Command> commands;

    {
        var command = new Command();
        command.setName("/start");
        command.setAction(update -> log.debug("Received command: {}", update.getMessage().getText()));
        commands = new ArrayList<>(List.of(command));
    }
    /// Теперь нужно понять как запоминать состояние и данные пользователя.
    /// затем понимать в каком он состоянии и что нам нужно делать когда он в этом состоянии.

    private Predicate<Command> commandMatchesUpdate(Update update) {
        return command -> Objects.equals(command.getName(), update.getMessage().getText());
    }

    private void processCommand(Update update) {
        commands.stream()
                .filter(commandMatchesUpdate(update))
                .findFirst()
                .ifPresent(command -> command.getAction().accept(update));
    }

    public void onMessageReceived(Update update) {
        processCommand(update);
    }

    public void onCallbackQueryReceived(Update update) {
    }
}

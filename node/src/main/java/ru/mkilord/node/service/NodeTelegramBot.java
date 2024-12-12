package ru.mkilord.node.service;

import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.mkilord.node.common.Command;
import ru.mkilord.node.common.CommandHandlerService;
import ru.mkilord.node.common.CommandRepository;

import java.util.ArrayList;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;


@FieldDefaults(level = PRIVATE, makeFinal = true)
@Slf4j
@Component
public class NodeTelegramBot implements CommandRepository {
    CommandHandlerService commandHandlerService;

    public NodeTelegramBot(CommandHandlerService commandHandlerService) {
        this.commandHandlerService = commandHandlerService;
        commandHandlerService.registerCommand(this);
    }

    public void onMessageReceived(Update update) {
        commandHandlerService.process(update);
    }

    public void onCallbackQueryReceived(Update update) {
    }

    @Override
    public List<Command> getCommands() {
        var start = Command.create()
                .name("/start")
                .action(context -> {
                    log.debug("Action command /start");
                    context.put("value", "start");
                })
                .withReply(_ -> log.debug("/start action reply 1"))
                .withReply(_ -> log.debug("/start action reply 2"))
                .build();

        var end = Command.create()
                .name("/end")
                .action(context -> log.debug("Action command /end"))
                .withReply(_ -> log.debug("/end action reply 1"))
                .withReply(_ -> log.debug("/end action reply 2"))
                .build();
        return new ArrayList<>(List.of(start, end));
    }
}

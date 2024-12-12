package ru.mkilord.node.service;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
@AllArgsConstructor
public class NodeTelegramBot implements CommandRepository {
    CommandHandlerService commandHandlerService;
    ProducerService producerService;

    @PostConstruct
    public void registerCommand() {
        commandHandlerService.registerCommand(this);
    }

    public void onMessageReceived(Update update) {
        var isUnknownCommand = commandHandlerService.process(update);
        if (isUnknownCommand) {
            var outMsg = SendMessage
                    .builder()
                    .chatId(update.getMessage().getChatId())
                    .text("Неверная команда! \nВведите /help чтобы получить справку.")
                    .build();
            sendMessage(outMsg);
        }
    }

    public void onCallbackQueryReceived(Update update) {
    }

    public void sendMessage(SendMessage message) {
        producerService.produceAnswer(message);
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

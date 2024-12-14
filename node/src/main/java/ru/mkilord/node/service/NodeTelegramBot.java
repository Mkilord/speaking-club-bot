package ru.mkilord.node.service;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.mkilord.node.TextUtils;
import ru.mkilord.node.common.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

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

    public void sendMessage(MessageContext context, String message) {
        var outMsg = SendMessage.builder().chatId(context.getChatId()).text(message).build();
        producerService.produceAnswer(outMsg);
    }


    @Override
    public List<Command> getCommands() {
        var start = Command.create()
                .name("/start")
                .info("Используйте чтобы начать.")
                .action(context -> {
                    log.debug("Action command /start");
                    context.put("value", "start");
                    return true;
                })
                .reply(context -> {
                    log.debug("/start action reply 1");
                    return true;
                })
                .reply(_ -> {
                    log.debug("/start action reply 2");
                    return true;
                })
                .build();
        var feedback = Command.create()
                .name("/feedback")
                .info("Чтобы пройти опрос.")
                .access(UserRole.ADMIN)
                .action(context -> {
                    log.debug("Action command /feedback");
                    sendMessage(context, "Поиск опросов!");
                    var bool = new Random().nextBoolean();
                    if (bool) {
                        sendMessage(context, "Введите оценку клуба немецкого от 0 до 10:");
                        return true;
                    } else {
                        sendMessage(context, "Доступные опросы не найдены!");
                    }
                    return bool;
                }).reply(context -> {
                    log.debug("Action command /feedback reply" + context.getUpdate().getMessage().getText());
                    var msg = context.getUpdate().getMessage().getText();
                    if (TextUtils.isInRange(msg, 0, 10)) {
                        sendMessage(context, "Оценка записана!");
                        log.debug("Wrong message!" + context.getUpdate().getMessage().getText());
                        return true;
                    } else {
                        sendMessage(context, "Введите число от 0 до 10!");
                        return false;
                    }
                }).build();
        var end = Command.create()
                .name("/end")
                .info("Чтобы апнуть права хахахах")
                .action(context -> {
                    context.setRole(UserRole.ADMIN.get());
                    log.debug("Action command /end");
                    return true;
                })
                .reply(_ -> {
                    log.debug("/end action reply 1");
                    return true;
                })
                .reply(_ -> {
                    log.debug("/end action reply 2");
                    return true;
                })
                .build();
        var simpleCommands = new ArrayList<>(List.of(start, end, feedback));

        var help = Command.create()
                .name("/help")
                .action(context -> {
                    var outMsg = new StringBuilder();
                    simpleCommands.stream().filter(command ->
                                    (command.getRole().equals(context.getRole()))
                                    && Objects.nonNull(command.getInfo()))
                            .forEach(command -> outMsg
                                    .append(command.getName())
                                    .append(" ")
                                    .append(command.getInfo())
                                    .append("\n"));
                    sendMessage(context, outMsg.toString());
                    return true;
                })
                .build();
        simpleCommands.add(help);
        return new ArrayList<>(simpleCommands);
    }
}

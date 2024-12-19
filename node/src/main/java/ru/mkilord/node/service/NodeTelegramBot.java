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
import static ru.mkilord.node.common.Step.*;


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
        var register = Command.create("/register")
                .info("Используйте чтобы зарегистрироваться")
                .action(context -> {
                    sendMessage(context, "Привет! Давай познакомимся!");
                    sendMessage(context, "Введите имя");
                })
                .reply(context -> {
                    var msg = context.getMessageText();
                    if (msg.isBlank()) {
                        sendMessage(context, "Введите, имя размером больше чем 1 символ.");
                        return REPEAT;
                    }
                    context.put("name", msg);
                    sendMessage(context, "Введите фамилию");
                    return NEXT;
                })
                .reply(context -> {
                    var msg = context.getMessageText();
                    if (msg.isBlank()) {
                        sendMessage(context, "Введите, фамилию размером больше чем 1 символ.");
                        return REPEAT;
                    }
                    context.put("surname", msg);
                    sendMessage(context, "Введите номер телефона");
                    return NEXT;
                })
                .reply(context -> {
                    var msg = context.getMessageText();
                    if (!TextUtils.isProneNumber(msg)) {
                        sendMessage(context, "Введите, телефон в формате: \"79106790783\"");
                        return REPEAT;
                    }
                    context.put("phone", msg);
                    sendMessage(context, "Введите email");
                    return NEXT;
                })
                .reply(context -> {
                    var email = context.getMessageText();
                    var name = context.getValue("name");
                    var surname = context.getValue("surname");
                    var phone = context.getValue("phone");

                    sendMessage(context, "ФИ: %s %s; Тел: %s; Email: %s".formatted(name, surname, phone, email));

                    return TERMINATE;
                }).build();
        var start = Command.create("/start")
                .info("Используйте чтобы начать.")
                .action(context -> {
                    log.debug("Action command /start");
                    context.put("value", "start");
                    return NEXT;
                })
                .reply(context -> {
                    log.debug("/start action reply 1");
                    return NEXT;
                })
                .reply(_ -> {
                    log.debug("/start action reply 2");
                    return NEXT;
                }).build();
        var feedback = Command.create("/feedback")
                .info("Чтобы пройти опрос.")
                .access(UserRole.ADMIN)
                .action(context -> {
                    log.debug("Action command /feedback");
                    sendMessage(context, "Поиск опросов!");
                    var isOk = new Random().nextBoolean();
                    if (isOk) {
                        sendMessage(context, "Введите оценку клуба немецкого от 0 до 10:");
                        return NEXT;
                    }
                    sendMessage(context, "Доступные опросы не найдены!");
                    return TERMINATE;
                }).reply(context -> {
                    log.debug("Action command /feedback reply" + context.getUpdate().getMessage().getText());
                    var msg = context.getUpdate().getMessage().getText();
                    if (TextUtils.isRange(msg, 0, 10)) {
                        sendMessage(context, "Оценка записана!");
                        log.debug("Wrong message!" + context.getUpdate().getMessage().getText());
                        return NEXT;
                    }
                    sendMessage(context, "Введите число от 0 до 10!");
                    return REPEAT;
                }).build();
        var end = Command.create("/end")
                .info("Чтобы апнуть права хахахах")
                .action(context -> {
                    context.setUserRole(UserRole.ADMIN.get());
                    log.debug("Action command /end");
                    return NEXT;
                })
                .reply(_ -> {
                    log.debug("/end action reply 1");
                    return NEXT;
                })
                .reply(_ -> {
                    log.debug("/end action reply 2");
                    return NEXT;
                }).build();
        var simpleCommands = new ArrayList<>(List.of(register, start, end, feedback));


        var help = Command.create("/help")
                .action(context -> {
                    var strBuilder = new StringBuilder();
                    simpleCommands.stream()
                            .filter(command ->
                                    (command.getRoles().contains(context.getUserRole()))
                                            && Objects.nonNull(command.getInfo()))
                            .forEach(command -> strBuilder
                                    .append(command.getName())
                                    .append(" - ")
                                    .append(command.getInfo())
                                    .append("\n"));
                    var outMsg = strBuilder.toString();
                    if (outMsg.isEmpty()) {
                        outMsg = "Для вас нет доступных команд!";
                    }
                    sendMessage(context, outMsg);
                    return TERMINATE;
                }).build();
        simpleCommands.add(help);
        return new ArrayList<>(simpleCommands);
    }
}

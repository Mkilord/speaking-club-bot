package ru.mkilord.node.service;

import jakarta.annotation.PostConstruct;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.mkilord.node.common.Command;
import ru.mkilord.node.common.MessageContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static lombok.AccessLevel.PRIVATE;


@FieldDefaults(level = PRIVATE/*, makeFinal = true*/)
@Slf4j
@Component
public class NodeTelegramBot {

    ArrayList<Command> commands;

    ArrayList<MessageContext> contexts = new ArrayList<>();

    {
        var start = Command.builder()
                .name("/start")
                .action(context -> {
                    log.debug("Action command /start");
                })
                .withReply(_ -> log.debug("/start action reply 1"))
                .withReply(_ -> log.debug("/start action reply 2"))
                .build();

        var end = Command.builder()
                .name("/end")
                .action(context -> log.debug("Action command /end"))
                .withReply(_ -> log.debug("/end action reply 1"))
                .withReply(_ -> log.debug("/end action reply 2"))
                .build();
        commands = new ArrayList<>(List.of(start, end));
    }

    @PostConstruct
    public void init() {}

    private Predicate<Command> commandMatchesUpdate(MessageContext messageContext) {
        return command -> Objects.equals(command.getName(), messageContext.getUpdate().getMessage().getText());
    }

    public MessageContext getContext(Update update) {
        return contexts.stream()
                .filter(context -> update.getMessage().getChatId().equals(context.getChatId()))
                .findFirst()
                .map(context -> {
                    context.setUpdate(update);
                    return context;
                }).orElseGet(() -> createContext(update));
    }

    public MessageContext createContext(Update update) {
        var context = MessageContext.builder()
                .chatId(update.getMessage().getChatId())
                .update(update)
                .build();
        contexts.add(context);
        return context;
    }

    private boolean processReply(MessageContext context) {
        if (!context.hasReply()) return false;
        var reply = context.getReply();
        reply.getAction().accept(context);
        context.setReply(reply.getNextReplay());
        return true;
    }

    private void processCommand(MessageContext context) {
        commands.stream()
                .filter(commandMatchesUpdate(context))
                .findFirst()
                .ifPresent(command -> {
                    command.getAction().accept(context);
                    context.setReply(command.getReply());
                });
    }

    public void onMessageReceived(Update update) {
        var context = getContext(update);
        if (processReply(context)) return;
        processCommand(context);
    }

    public void onCallbackQueryReceived(Update update) {
    }
}

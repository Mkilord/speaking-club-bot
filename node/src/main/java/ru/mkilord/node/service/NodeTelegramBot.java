package ru.mkilord.node.service;

import jakarta.annotation.PostConstruct;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.mkilord.node.common.Command;
import ru.mkilord.node.common.MessageContext;
import ru.mkilord.node.common.Reply;

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

    ArrayList<Reply> replies = new ArrayList<>();

    ArrayList<MessageContext> contexts = new ArrayList<>();

    {
        var command = new Command();
        command.setName("/start");
        command.setAction(context -> log.debug("Received command: {}", context.getUpdate().getMessage().getText()));
        var reply = new Reply();
        reply.setAction(messageContext -> log.debug("Received reply: {}", messageContext.getUpdate().getMessage().getText()));
        command.setReply(reply);
        commands = new ArrayList<>(List.of(command));
    }

    @PostConstruct
    public void init() {
        compileCommands();
    }

    /// Теперь нужно понять как запоминать состояние и данные пользователя.
    /// затем понимать в каком он состоянии и что нам нужно делать когда он в этом состоянии.

    private Predicate<Command> commandMatchesUpdate(MessageContext messageContext) {
        return command -> Objects.equals(command.getName(), messageContext.getUpdate().getMessage().getText());
    }

    private void compileCommands() {
        commands.forEach(command -> replies.add(command.getReply()));
    }

    private void processCommand(MessageContext context) {
        commands.stream()
                .filter(commandMatchesUpdate(context))
                .findFirst()
                .ifPresent(c -> c.getAction().accept(context));
    }

    private void processReply(MessageContext context) {
        replies.stream()
                .filter(replyMatchesUpdate(context))
                .findFirst()
                .ifPresent(reply -> reply.getAction().accept(context));
    }


    private Predicate<Reply> replyMatchesUpdate(MessageContext context) {
        return reply -> Objects.equals(reply.getName(), context.getUpdate().getMessage().getText());
    }

    public MessageContext findCurrentContext(Update update) {
        return contexts.stream().filter(messageContext -> update.getMessage().getFrom().getId().equals(messageContext.getUserId())).findFirst()
                .orElseGet(() -> {
                    var context = MessageContext.builder()
                            .userId(update.getMessage().getFrom().getId())
                            .chatId(update.getMessage().getChatId())
                            .update(update)
                            .build();
                    contexts.add(context);
                    return context;
                });
    }

    public void onMessageReceived(Update update) {
        var context = findCurrentContext(update);
        processCommand(context);
        //Тут нужно обрабатывать команду. Чтобы можно было терять контекст сообщения.
        processReply(context);
    }


    public void onCallbackQueryReceived(Update update) {
    }
}

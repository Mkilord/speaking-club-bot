package ru.mkilord.node.common;

import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@FieldDefaults(level = PRIVATE)
@Service
public class CommandHandlerService {

    //    List<Command> commands;
    Map<String, Reply> replies;
    Map<String, Command> commandMap;
    final ArrayList<MessageContext> contexts = new ArrayList<>();

    public void registerCommand(CommandRepository commandRepository) {
        var commands = commandRepository.getCommands();
        commandMap = commands.stream().collect(Collectors.toMap(Command::getName, command -> command));
        replies = commands.stream()
                .map(Command::extractReplies)
                .flatMap(repliesMap -> repliesMap.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (reply1, _) -> reply1));
    }

    private Predicate<Command> commandMatchesUpdate(MessageContext messageContext) {
        return command -> Objects.equals(command.getName(), messageContext.getUpdate().getMessage().getText());
    }

    private MessageContext getContext(Update update) {
        return contexts.stream()
                .filter(context -> update.getMessage().getChatId().equals(context.getChatId()))
                .findFirst()
                .map(context -> {
                    context.setUpdate(update);
                    return context;
                }).orElseGet(() -> createContext(update));
    }

    private MessageContext createContext(Update update) {
        var context = MessageContext.builder()
                .chatId(update.getMessage().getChatId())
                .update(update)
                .build();
        contexts.add(context);
        return context;
    }

    private boolean processReply(MessageContext context) {
        if (!context.hasReply()) return false;
        var replyId = context.getReplyId();
        var reply = replies.get(replyId);
        reply.getAction().accept(context);
        reply.getNextReplay().ifPresentOrElse(reply1 -> context.setReplyId(reply1.getId()), () -> {
            context.setReplyId(null);
            context.clear();
        });
        return true;
    }

    private boolean processCommand(MessageContext context) {
        var message = context.getUpdate().getMessage().getText();
        var commandOpt = Optional.ofNullable(commandMap.get(message));
        return commandOpt.map(command -> {
                    command.getAction().accept(context);
                    context.setReplyId(command.getReply().getId());
                    return true;
                })
                .orElse(false);
    }

    public boolean process(Update update) {
        var context = getContext(update);
        if (processReply(context)) return false;
        return !processCommand(context);
    }
}

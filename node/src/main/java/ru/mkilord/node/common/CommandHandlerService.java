package ru.mkilord.node.common;

import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Service
@FieldDefaults(level = PRIVATE)
public class CommandHandlerService {

    Map<String, Reply> replyMap;
    List<Command> commandsList;

    final Map<Long, MessageContext> contextMap = new HashMap<>();

    public void registerCommand(CommandRepository commandRepository) {
        commandsList = commandRepository.getCommands();
        replyMap = commandsList.stream()
                .map(Command::extractReplies)
                .flatMap(repliesMap -> repliesMap.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (reply1, _) -> reply1));
    }

    private MessageContext getContext(Update update) {
        var chatId = update.getMessage().getChatId();
        return Optional.ofNullable(contextMap.get(chatId))
                .map(context -> {
                    context.setUpdate(update);
                    return context;
                }).orElseGet(() -> createContext(update));
    }

    private MessageContext createContext(Update update) {
        var context = MessageContext.builder()
                .chatId(update.getMessage().getChatId())
                .userRole(UserRole.USER.get())
                .update(update)
                .build();
        contextMap.put(context.getChatId(), context);
        return context;
    }

    private boolean processReply(MessageContext context) {
        if (!context.hasReply()) return false;
        return Optional.ofNullable(replyMap.get(context.getCurrentReplyId()))
                .map(reply -> reply.processAction(context))
                .orElse(false);
    }

    private boolean processCommand(MessageContext context) {
        return commandsList.stream()
                .filter(command -> command.matchConditions(context))
                .findFirst()
                .map(command -> command.processAction(context)).orElse(false);
    }

    public boolean process(Update update) {
        var context = getContext(update);
        if (processCommand(context)) return false;
        return !processReply(context);
    }
}

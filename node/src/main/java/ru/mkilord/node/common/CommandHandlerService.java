package ru.mkilord.node.common;

import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@FieldDefaults(level = PRIVATE)
@Service
public class CommandHandlerService {

    Map<String, Reply> replyMap;
    Map<String, Command> commandMap;

    final Map<Long, MessageContext> contextMap = new HashMap<>();

    public void registerCommand(CommandRepository commandRepository) {
        var commands = commandRepository.getCommands();
        commandMap = commands.stream().collect(Collectors.toMap(Command::getName, command -> command));
        replyMap = commands.stream()
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
                .update(update)
                .build();
        contextMap.put(context.getChatId(), context);
        return context;
    }

    private boolean processReply(MessageContext context) {
        if (!context.hasReply()) return false;
        var replyId = context.getReplyId();
        return Optional.ofNullable(replyMap.get(replyId)).map((reply -> {
            var isContinue = reply.getAction().apply(context);
            if (isContinue) {
                reply.getNextReplay().ifPresentOrElse(nextReply -> context.setReplyId(nextReply.getId()), context::clear);
            }
            return true;
        })).orElse(false);
    }

    private boolean processCommand(MessageContext context) {
        var message = context.getUpdate().getMessage().getText();
        return Optional.ofNullable(commandMap.get(message))
                .map(command -> {
                    var isContinue = command.getAction().apply(context);
                    if (isContinue) {
                        context.setReplyId(command.getReply().getId());
                    } else {
                        context.clear();
                    }
                    return true;
                })
                .orElse(false);
    }

    public boolean process(Update update) {
        var context = getContext(update);
        if (processCommand(context)) return false;
        return !processReply(context);
    }
}

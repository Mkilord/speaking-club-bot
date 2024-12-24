package ru.mkilord.node.command;

import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.mkilord.node.model.Role;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Service
@FieldDefaults(level = PRIVATE)
public class CommandHandler {

    Map<String, Reply> replyMap;
    List<Command> commandsList;

    final Map<Long, MessageContext> contextMap = new HashMap<>();

    Consumer<MessageContext> unknownCommandCallback;

    public void registerCommands(CommandCatalog commandCatalog, Consumer<MessageContext> unknownCommandCallback) {
        this.unknownCommandCallback = unknownCommandCallback;
        commandsList = commandCatalog.setCommands();
        replyMap = commandsList.stream()
                .map(Command::extractReplies)
                .flatMap(repliesMap -> repliesMap.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (reply1, _) -> reply1));
    }

    private Long getChatId(Update update) {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        return update.getMessage().getChatId();
    }

    private MessageContext getContext(Update update) {
        var chatId = getChatId(update);
        return Optional.ofNullable(contextMap.get(chatId))
                .map(context -> {
                    context.setChatId(chatId);
                    return context.setUpdate(update);
                })
                .orElseGet(() -> createContext(update));
    }

    private MessageContext createContext(Update update) {
        var context = MessageContext.builder()
                .chatId(getChatId(update))
                .userRole(Role.USER.toString())
                .update(update)
                .build();
        contextMap.put(context.getChatId(), context);
        return context;
    }

    private boolean processReply(MessageContext context) {
        if (!context.hasReply()) return false;
        return Optional.ofNullable(replyMap.get(context.getReplyId()))
                .map(reply -> reply.processAction(context))
                .orElse(false);
    }

    private boolean processCommand(MessageContext context) {
        return commandsList.stream()
                .filter(command -> command.matchConditions(context))
                .findFirst()
                .map(command -> command.processAction(context)).orElse(false);
    }

    public void process(Update update) {
        var context = getContext(update);
        if (processCommand(context) || processReply(context)) {
            return;
        }
        unknownCommandCallback.accept(context);
    }
}

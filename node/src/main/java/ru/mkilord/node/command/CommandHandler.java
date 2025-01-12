package ru.mkilord.node.command;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.mkilord.node.model.enums.Role;
import ru.mkilord.node.model.User;
import ru.mkilord.node.service.impl.UserService;

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
@RequiredArgsConstructor
public class CommandHandler {

    Map<String, Reply> replyMap;
    List<Command> commandsList;

    final Map<Long, MessageContext> contextMap = new HashMap<>();
    final UserService userService;

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

    private Long getUserId(Update update) {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getFrom().getId();
        }
        return update.getMessage().getChatId();
    }

    private MessageContext getContext(Update update) {
        var chatId = getChatId(update);
        return Optional.ofNullable(contextMap.get(chatId))
                .map(context -> context.setUpdate(update))
                .orElseGet(() -> createContext(update));
    }

    private MessageContext createContext(Update update) {
        var telegramId = getUserId(update);
        var chatId = getChatId(update);
        var user = userService.getUserById(telegramId);
        var context = user.map(user1 -> {
                    //Нужно сделать проверку на смену никнейма.
                    if (!user1.getChatId().equals(chatId)) {
                        user1.setChatId(chatId);
                        userService.update(user1);
                    }
                    return MessageContext.builder()
                            .user(user1)
                            .update(update)
                            .build();
                })
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setRole(Role.USER);
                    newUser.setTelegramId(telegramId);
                    newUser.setChatId(chatId);
                    newUser = userService.save(newUser);
                    return MessageContext.builder()
                            .user(newUser)
                            .update(update)
                            .build();
                });
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

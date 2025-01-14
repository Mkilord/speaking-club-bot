package ru.mkilord.node.command;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.mkilord.node.config.BotConfig;
import ru.mkilord.node.model.User;
import ru.mkilord.node.model.enums.Role;
import ru.mkilord.node.service.impl.UserService;
import ru.mkilord.node.util.UpdateUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;
import static ru.mkilord.node.util.UpdateUtils.getChatIdFromUpdate;
import static ru.mkilord.node.util.UpdateUtils.getUserIdFromUpdate;

@Slf4j
@FieldDefaults(level = PRIVATE)
@RequiredArgsConstructor
@Service
public class CommandHandler {

    Map<String, Reply> replyMap;
    List<Command> commandsList;

    final UserService userService;

    final BotConfig botConfig;

    final ContextFlow contextFlow = new ContextFlow(200);

    Consumer<MessageContext> unknownCommandCallback;

    public void registerCommands(CommandCatalog commandCatalog, Consumer<MessageContext> unknownCommandCallback) {
        this.unknownCommandCallback = unknownCommandCallback;
        commandsList = commandCatalog.setCommands();
        replyMap = commandsList.stream()
                .map(Command::extractReplies)
                .flatMap(repliesMap -> repliesMap.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (reply1, _) -> reply1));
    }

    public void disposeContext(MessageContext context) {
        contextFlow.remove(context);
    }

    private MessageContext lookupOrCreateContext(Update update) {
        var chatId = getChatIdFromUpdate(update);
        return contextFlow.getContext(chatId)
                .map(context -> context.setUpdate(update))
                .orElseGet(() -> createContext(update));
    }

    private MessageContext createContext(Update update) {
        var telegramId = getUserIdFromUpdate(update);
        var chatId = getChatIdFromUpdate(update);
        var userOpt = userService.getUserById(telegramId);
        var context = userOpt
                .map(user -> restoreUser(user, update, chatId))
                .orElseGet(() -> createContextForNewUser(update, telegramId, chatId));
        contextFlow.addContext(context.getChatId(), context);
        return context;
    }

    private void setRoleIfModerator(User user, long telegramId) {
        if (Long.parseLong(botConfig.getAdminId()) == telegramId)
            user.setRole(Role.MODERATOR);
        else
            user.setRole(Role.USER);

    }

    private MessageContext createContextForNewUser(Update update, long telegramId, long chatId) {
        var newUser = new User();

        setRoleIfModerator(newUser, telegramId);

        newUser.setTelegramId(telegramId);
        newUser.setChatId(chatId);

        newUser = userService.save(newUser);

        return MessageContext.builder()
                .user(newUser)
                .update(update)
                .build();
    }

    private MessageContext restoreUser(User user, Update update, long chatId) {
        var username = UpdateUtils.getUsernameFromUpdate(update);

        if (!user.getUsername().equals(username)) {
            user.setUsername(username);
            userService.update(user);
        }
        if (!user.getChatId().equals(chatId)) {
            user.setChatId(chatId);
            userService.update(user);
        }

        return MessageContext.builder()
                .user(user)
                .update(update)
                .build();
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
        var context = lookupOrCreateContext(update);
        if (processCommand(context) || processReply(context)) return;
        unknownCommandCallback.accept(context);
    }
}

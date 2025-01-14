package ru.mkilord.node.command.context;

import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.mkilord.node.config.BotConfig;
import ru.mkilord.node.model.User;
import ru.mkilord.node.model.enums.Role;
import ru.mkilord.node.service.impl.UserService;
import ru.mkilord.node.util.UpdateUtils;

import static lombok.AccessLevel.PRIVATE;
import static ru.mkilord.node.util.UpdateUtils.getChatIdFromUpdate;
import static ru.mkilord.node.util.UpdateUtils.getUserIdFromUpdate;

@Component
@FieldDefaults(level = PRIVATE, makeFinal = true)
@AllArgsConstructor
public class ContextFlow {

    BotConfig botConfig;

    UserService userService;

    final ContextCollection contextCollection = new ContextCollection(200);

    public void disposeContext(MessageContext context) {
        contextCollection.remove(context);
    }

    public MessageContext lookupOrCreateContext(Update update) {
        var chatId = getChatIdFromUpdate(update);
        return contextCollection.getContext(chatId)
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
        contextCollection.addContext(context.getChatId(), context);
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
        newUser.setUsername(UpdateUtils.getUsernameFromUpdate(update));

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
}

package ru.mkilord.node.util;


import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

public class UpdateUtils {
    public static Long getUserIdFromUpdate(Update update) {
        return getUserFromUpdate(update).getId();
    }

    public static String getUsernameFromUpdate(Update update) {
        return getUserFromUpdate(update).getUserName();
    }

    public static Long getChatIdFromUpdate(Update update) {
        if (update.hasCallbackQuery())
            return update.getCallbackQuery().getMessage().getChatId();
        return update.getMessage().getChatId();
    }

    public static User getUserFromUpdate(Update update) {
        if (update.hasCallbackQuery())
            return update.getCallbackQuery().getFrom();
        return update.getMessage().getFrom();
    }
}

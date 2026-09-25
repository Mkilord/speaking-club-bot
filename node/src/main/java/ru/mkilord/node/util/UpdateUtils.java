package ru.mkilord.node.util;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

public final class UpdateUtils {

    private UpdateUtils() {
    }

    public static long chatId(Update update) {
        return update.hasCallbackQuery()
                ? update.getCallbackQuery().getMessage().getChatId()
                : update.getMessage().getChatId();
    }

    public static User sender(Update update) {
        return update.hasCallbackQuery()
                ? update.getCallbackQuery().getFrom()
                : update.getMessage().getFrom();
    }
}

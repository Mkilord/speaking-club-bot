package ru.mkilord.node.fsm;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.concurrent.atomic.AtomicInteger;

/** Builds Telegram updates for tests. */
public final class TestUpdates {

    private static final AtomicInteger UPDATE_ID = new AtomicInteger();

    private TestUpdates() {
    }

    public static Update text(long chatId, String text) {
        var message = message(chatId);
        message.setText(text);
        var update = new Update();
        update.setUpdateId(UPDATE_ID.incrementAndGet());
        update.setMessage(message);
        return update;
    }

    public static Update callback(long chatId, String data) {
        var callback = new CallbackQuery();
        callback.setId("cb" + UPDATE_ID.get());
        callback.setFrom(new User(chatId, "Test", false));
        callback.setMessage(message(chatId));
        callback.setData(data);
        var update = new Update();
        update.setUpdateId(UPDATE_ID.incrementAndGet());
        update.setCallbackQuery(callback);
        return update;
    }

    private static Message message(long chatId) {
        var message = new Message();
        message.setMessageId(1);
        message.setChat(new Chat(chatId, "private"));
        message.setFrom(new User(chatId, "Test", false));
        return message;
    }
}

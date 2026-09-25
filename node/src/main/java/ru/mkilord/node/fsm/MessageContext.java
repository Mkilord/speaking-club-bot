package ru.mkilord.node.fsm;

import lombok.Getter;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.mkilord.node.dialog.DialogState;
import ru.mkilord.node.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Everything a handler needs to process one update: the update, the user,
 * the persisted dialog state and the list of messages to send.
 * <p>
 * Messages are not sent immediately. They are collected here and published
 * after the database transaction commits.
 */
public class MessageContext {

    @Getter
    private final Update update;
    @Getter
    private final User user;
    private final DialogState state;
    private final List<SendMessage> outbox = new ArrayList<>();

    public MessageContext(Update update, User user, DialogState state) {
        this.update = update;
        this.user = user;
        this.state = state;
    }

    public long getChatId() {
        return state.getChatId();
    }

    public boolean isCallback() {
        return update.hasCallbackQuery();
    }

    /** Message text or callback data of the button. */
    public String getText() {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getData();
        }
        var text = update.getMessage().getText();
        return text == null ? "" : text.strip();
    }

    Optional<String> getReplyId() {
        return Optional.ofNullable(state.getReplyId());
    }

    void setReplyId(String replyId) {
        state.setReplyId(replyId);
    }

    /** Ends the current dialog and forgets collected values. */
    public void clear() {
        state.reset();
    }

    public void put(String key, Object value) {
        state.getValues().put(key, String.valueOf(value));
    }

    public String get(String key) {
        var value = state.getValues().get(key);
        if (value == null) {
            throw new IllegalStateException("Dialog value '" + key + "' is missing");
        }
        return value;
    }

    public String getOrNull(String key) {
        return state.getValues().get(key);
    }

    public long getLong(String key) {
        return Long.parseLong(get(key));
    }

    public void send(String text) {
        sendTo(getChatId(), text, null);
    }

    public void send(String text, InlineKeyboardMarkup keyboard) {
        sendTo(getChatId(), text, keyboard);
    }

    public void sendTo(long chatId, String text) {
        sendTo(chatId, text, null);
    }

    private void sendTo(long chatId, String text, InlineKeyboardMarkup keyboard) {
        outbox.add(SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboard)
                .build());
    }

    public List<SendMessage> getOutbox() {
        return Collections.unmodifiableList(outbox);
    }
}

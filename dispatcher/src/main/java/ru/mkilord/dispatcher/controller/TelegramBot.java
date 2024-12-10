package ru.mkilord.dispatcher.controller;

import jakarta.annotation.PostConstruct;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.Serializable;

import static lombok.AccessLevel.PRIVATE;

@FieldDefaults(makeFinal = true, level = PRIVATE)
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    String userName;
    UpdateController updateController;

    public TelegramBot(String token, String username, UpdateController updateController) {
        super(token);
        this.userName = username;
        this.updateController = updateController;

        log.debug("Bot initialized with username: %s".formatted(username));
    }

    @PostConstruct
    public void init() {
        updateController.registerBot(this);
    }

    @Override
    public String getBotUsername() {
        return userName;
    }

    @Override
    public void onUpdateReceived(Update update) {
        updateController.processUpdate(update);
    }

    public void answerCallbackQuery(String callbackQueryId) {
        var answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQueryId);
        execute(answer);
    }

    public <T extends Serializable, Method extends BotApiMethod<T>> T execute(Method method) {
        try {
            return super.execute(method);
        } catch (TelegramApiException e) {
            log.error("Could not execute bot API method", e);
            return null;
        }
    }

    public void sendMsg(SendMessage message) {
        try {
            super.execute(message);
        } catch (TelegramApiException e) {
            log.error("Could not execute bot API method", e);
        }
    }
}

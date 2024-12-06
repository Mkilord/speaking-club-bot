package ru.mkilord.dispatcher.service;

import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface AnswerConsumer {
    void consume(SendMessage sendMessage);

    void consume(AnswerCallbackQuery answerCallbackQuery);
}

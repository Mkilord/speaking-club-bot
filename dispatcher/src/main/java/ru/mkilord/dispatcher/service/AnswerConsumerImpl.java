package ru.mkilord.dispatcher.service;

import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public class AnswerConsumerImpl implements AnswerConsumer {
    @Override
    public void consume(SendMessage sendMessage) {

    }

    @Override
    public void consume(AnswerCallbackQuery answerCallbackQuery) {

    }
}

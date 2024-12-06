package ru.mkilord.dispatcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.mkilord.dispatcher.controller.TelegramBot;

@SpringBootApplication
public class DispatcherApplication {

    public static void main(String[] args) {
        SpringApplication.run(DispatcherApplication.class, args);
    }

}

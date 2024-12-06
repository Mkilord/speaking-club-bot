package ru.mkilord.dispatcher.config;

import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.mkilord.dispatcher.controller.TelegramBot;
import ru.mkilord.dispatcher.controller.UpdateController;

import static lombok.AccessLevel.PRIVATE;

@Configuration
@FieldDefaults(level = PRIVATE)
public class BotConfig {
    @Value("${bot.token}")
    String token;
    @Value("${bot.username}")
    String username;

    @Bean
    public TelegramBotsApi telegramBotsApi(UpdateController updateController) throws TelegramApiException {
        var botsApi = new TelegramBotsApi(DefaultBotSession.class);
        var bot = new TelegramBot(token, username, updateController);
        bot.init();
        botsApi.registerBot(bot);
        return botsApi;
    }


}

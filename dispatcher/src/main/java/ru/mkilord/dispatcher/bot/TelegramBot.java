package ru.mkilord.dispatcher.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.mkilord.dispatcher.config.BotProperties;

/**
 * Long polling client. Does no business logic: every update goes to {@link UpdateRouter}.
 */
@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final String username;
    private final UpdateRouter router;

    public TelegramBot(BotProperties properties, UpdateRouter router) {
        super(properties.token());
        this.username = properties.username();
        this.router = router;
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public void onUpdateReceived(Update update) {
        router.route(update, this);
    }
}

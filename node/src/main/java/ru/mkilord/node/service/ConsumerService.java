package ru.mkilord.node.service;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface ConsumerService {
    void consumeMessageUpdates(Update update);
}

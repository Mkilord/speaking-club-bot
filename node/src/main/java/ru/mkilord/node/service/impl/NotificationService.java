package ru.mkilord.node.service.impl;

import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.mkilord.node.model.Meet;
import ru.mkilord.node.model.User;
import ru.mkilord.node.service.ProducerService;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@FieldDefaults(level = PRIVATE, makeFinal = true)
@AllArgsConstructor
@Service
public class NotificationService {

    ProducerService producerService;

    @Transactional
    public void notifyUsersAboutMeet(List<User> subscribers, String message) {
        subscribers.forEach(user -> sendNotification(user, message));
    }

    private void sendNotification(User user, String message) {
        var outMsg = SendMessage.builder()
                .chatId(user.getChatId())
                .text(message)
                .build();
        producerService.produceAnswer(outMsg);
        log.info("Уведомление отправлено пользователю {} (chatId={}): {}", user.getUsername(), user.getChatId(), message);

    }

    public static String generateNotificationFrom(Meet meet) {
        var message = "Здравствуйте! У клуба %s назначена новая встреча: %s на %s %s. Не пропустите!";
        var meetStatus = meet.getStatus();

        switch (meetStatus) {
            case CANCELLED -> message = "Здравствуйте! У клуба %s встреча: %s на %s %s отменена!";
            case PUBLISHED -> message = "Здравствуйте! У клуба %s назначена новая встреча: %s на %s %s. Не пропустите!";
            default -> throw new RuntimeException("Undetected meet status: " + meetStatus);
        }

        return String.format(
                message,
                meet.getClub().getName(),
                meet.getName(),
                meet.getDate(),
                meet.getTime()
        );
    }
}


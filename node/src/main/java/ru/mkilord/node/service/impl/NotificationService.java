package ru.mkilord.node.service.impl;

import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.mkilord.node.model.Meet;
import ru.mkilord.node.model.User;
import ru.mkilord.node.model.enums.MeetStatus;
import ru.mkilord.node.service.ProducerService;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@FieldDefaults(level = PRIVATE, makeFinal = true)
@AllArgsConstructor
@Service
public class NotificationService {

    ProducerService producerService;

    @Transactional
    public void notifyUsersAboutMeet(Meet meet, MeetStatus status) {
        if (!Hibernate.isInitialized(meet.getClub().getSubscribers())) {
            throw new LazyInitializationException("Subscribers not initialized for meet with ID: " + meet.getId());
        }
        var club = meet.getClub();
        var subscribers = club.getSubscribers();
        subscribers.forEach(user -> sendNotification(user, generateNotificationMessage(meet, status)));
    }


    private void sendNotification(User user, String message) {
        var outMsg = SendMessage.builder()
                .chatId(user.getChatId())
                .text(message)
                .build();
        producerService.produceAnswer(outMsg);
        log.info("Уведомление отправлено пользователю {} (chatId={}): {}", user.getUsername(), user.getChatId(), message);

    }

    private String generateNotificationMessage(Meet meet, MeetStatus meetStatus) {
        var message = "Здравствуйте! У клуба %s назначена новая встреча: %s на %s %s. Не пропустите!";
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


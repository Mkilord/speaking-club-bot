package ru.mkilord.node.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.mkilord.node.model.Meet;
import ru.mkilord.node.model.User;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Service
@FieldDefaults(level = PRIVATE, makeFinal = true)
@AllArgsConstructor
public class NotificationService {

    ProducerService producerService;
    MeetService meetService;

    @Transactional
    public void notifyUsersAboutNewMeet(User parent, long meetId) {
        log.info("Начало уведомления пользователей о новой встрече. parentId={}, meetId={}", parent.getTelegramId(), meetId);

        var meetOpt = meetService.getMeetById(meetId);
        if (meetOpt.isEmpty()) {
            return;
        }
        var meet = meetOpt.get();
        var club = meet.getClub();

        var subscribers = club.getSubscribers();
        subscribers.remove(parent);

        log.info("Найдено {} подписчиков клуба {} (исключая родителя)", subscribers.size(), club.getName());

        subscribers.forEach(user -> sendNotification(user, generateNotificationMessage(meet)));
    }

    private void sendNotification(User user, String message) {
        var outMsg = SendMessage.builder()
                .chatId(user.getChatId())
                .text(message)
                .build();
        producerService.produceAnswer(outMsg);
        log.info("Уведомление отправлено пользователю {} (chatId={}): {}", user.getUsername(), user.getChatId(), message);

    }

    private String generateNotificationMessage(Meet meet) {
        return String.format(
                "Здравствуйте! У клуба %s назначена новая встреча: %s на %s %s. Не пропустите!",
                meet.getClub().getName(),
                meet.getName(),
                meet.getDate(),
                meet.getTime()
        );
    }
}


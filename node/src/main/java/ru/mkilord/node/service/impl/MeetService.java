package ru.mkilord.node.service.impl;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import ru.mkilord.node.model.Meet;
import ru.mkilord.node.model.User;
import ru.mkilord.node.model.enums.MeetStatus;
import ru.mkilord.node.repository.MeetRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Service
@AllArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class MeetService {

    MeetRepository meetRepository;
    NotificationService notificationService;

    public Meet save(Meet meet) {
        log.info("Сохранение встречи: {}", meet.getName());
        meet.setStatus(MeetStatus.HIDDEN);
        return meetRepository.save(meet);
    }

    @Transactional
    public Optional<Meet> update(long id, Meet meetDetails) {
        return getMeetById(id).map(meet -> {
            meet.setName(meetDetails.getName());
            meet.setDate(meetDetails.getDate());
            meet.setTime(meetDetails.getTime());
            meet.setStatus(meetDetails.getStatus());
            meet.setClub(meetDetails.getClub());
            log.info("Обновление встречи с id {}: {}", id, meetDetails.getName());
            return meet;
        });
    }

    @Transactional
    public Optional<Meet> publicMeetByIdWithNotification(long id) {
        return getMeetById(id).map(meet -> {
            meet.setStatus(MeetStatus.PUBLISHED);
            var subscribers = new ArrayList<>(meet.getClub().getSubscribers());
            notificationService.notifyUsersAboutMeet(subscribers, NotificationService.generateNotificationFrom(meet));
            return meet;
        });
    }

    @Transactional
    public Optional<Meet> cancelMeetByIdWithNotification(long id) {
        return getMeetById(id).map(meet -> {
            meet.setStatus(MeetStatus.CANCELLED);
            var subscribers = new ArrayList<>(meet.getClub().getSubscribers());
            notificationService.notifyUsersAboutMeet(subscribers, NotificationService.generateNotificationFrom(meet));
            return meet;
        });
    }

    public List<Meet> getPublishedMeetsByClubId(long clubId) {
        return meetRepository.findMeetsByClubIdAndStatus(clubId, MeetStatus.PUBLISHED);
    }

    public List<Meet> getMeetsByClubIdAndStatus(long clubId, Set<MeetStatus> meetSet) {
        return meetRepository.findMeetsByClubIdAndStatusIn(clubId, meetSet);
    }

    public List<Meet> getMeetsByClubIdAndStatus(long clubId, MeetStatus status) {
        return meetRepository.findMeetsByClubIdAndStatus(clubId, status);
    }

    public List<Meet> getMeetsByClubId(long clubId) {
        return meetRepository.findMeetsByClubId(clubId);
    }

    public Optional<Meet> getMeetById(Long meetId) {
        return meetRepository.findById(meetId);
    }

    @Transactional
    public Optional<Meet> getMeetWithRegisteredUsersById(Long meetId) {
        return getMeetById(meetId).map(meet -> {
            Hibernate.initialize(meet.getRegisteredUsers());
            return meet;
        });
    }

    public Iterable<Meet> getAllMeets() {
        log.info("Получение списка всех встреч");
        return meetRepository.findAll();
    }

    @Transactional
    public Optional<Meet> updateMeetStatus(Long meetId, MeetStatus status) {
        return getMeetById(meetId).map(meet -> {
            meet.setStatus(status);
            log.info("Обновление статуса встречи с id {} на {}", meetId, status);
            return meet;
        });
    }

    @Transactional
    public boolean deleteMeetById(Long meetId) {
        var meetOpt = meetRepository.findById(meetId);
        if (meetOpt.isEmpty()) {
            log.warn("Встреча с ID {} не найдена", meetId);
            return false;
        }
        var meet = meetOpt.get();
        if (meet.getStatus() == MeetStatus.PUBLISHED) return false;
        meet.getRegisteredUsers().clear();
        log.info("Удаление встречи с ID {}: {}", meetId, meet.getName());
        meetRepository.deleteById(meetId);
        return true;
    }

    @Transactional
    public Optional<Meet> addUserToMeet(Long meetId, User user) {
        return getMeetById(meetId).map(meet -> {
            var registeredUsers = meet.getRegisteredUsers();
            if (registeredUsers.contains(user)) {
                return meet;
            }
            if (registeredUsers.add(user)) {
                log.info("Добавлен пользователь {} к встрече {}", user.getUsername(), meet.getName());
            } else {
                log.warn("Пользователь {} уже зарегистрирован на встречу {}", user.getUsername(), meet.getName());
            }
            return meet;
        });
    }


    @Transactional
    public Optional<Meet> removeUserFromMeet(Long meetId, User user) {
        return getMeetById(meetId).map(meet -> {
            if (meet.getRegisteredUsers().remove(user)) {
                log.info("Удалён пользователь {} из встречи {}", user.getUsername(), meet.getName());
            } else {
                log.warn("Пользователь {} не был зарегистрирован на встречу {}", user.getUsername(), meet.getName());
            }
            return meet;
        });
    }
}

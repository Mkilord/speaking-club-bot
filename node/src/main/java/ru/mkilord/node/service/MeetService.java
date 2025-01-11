package ru.mkilord.node.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.mkilord.node.model.Meet;
import ru.mkilord.node.model.User;
import ru.mkilord.node.model.enums.MeetStatus;
import ru.mkilord.node.repository.MeetRepository;

import java.util.List;
import java.util.Optional;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Service
@AllArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class MeetService {

    MeetRepository meetRepository;

    public Meet save(Meet meet) {
        log.info("Сохранение встречи: {}", meet.getName());
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
            return meetRepository.save(meet);
        });
    }

    public List<Meet> getPublishedMeetsByClubId(long clubId) {
        return meetRepository.findMeetsByClubIdAndStatus(clubId, MeetStatus.PUBLISHED);
    }

    public List<Meet> getMeetsByClubId(long clubId) {
        return meetRepository.findMeetsByClubId(clubId);
    }

    public Optional<Meet> getMeetById(Long meetId) {
        return meetRepository.findById(meetId);
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
            return meetRepository.save(meet);
        });
    }

    @Transactional
    public Optional<Meet> addUserToMeet(Long meetId, User user) {
        return getMeetById(meetId).map(meet -> {
            var registeredUsers = meet.getRegisteredUsers();
            if (registeredUsers.add(user)) {
                log.info("Добавлен пользователь {} к встрече {}", user.getUsername(), meet.getName());
            } else {
                log.warn("Пользователь {} уже зарегистрирован на встречу {}", user.getUsername(), meet.getName());
            }
            return meetRepository.save(meet);
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
            return meetRepository.save(meet);
        });
    }
}

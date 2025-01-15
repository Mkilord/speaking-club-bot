package ru.mkilord.node.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mkilord.node.model.Club;
import ru.mkilord.node.model.User;
import ru.mkilord.node.repository.ClubRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClubService {

    ClubRepository clubRepository;

    public List<Club> getAll() {
        log.info("Получение всех клубов");
        return clubRepository.findAll();
    }

    public Optional<Club> getClubById(long id) {
        log.info("Поиск клуба с id {}", id);
        return clubRepository.findById(id);
    }

    public Club save(Club club) {
        log.info("Сохранение клуба: {}", club.getName());
        return clubRepository.save(club);
    }

    @Transactional
    public Optional<Club> update(long id, Club clubDetails) {
        return getClubById(id).map(club -> {
            club.setName(clubDetails.getName());
            club.setDescription(clubDetails.getDescription());
            log.info("Обновление клуба с id {}: {}", id, clubDetails.getName());
            return club;
        });
    }

    @Transactional
    public void addRating(Long clubId, int rating) {
        var clubOpt = getClubById(clubId);
        if (clubOpt.isEmpty()) return;

        var club = clubOpt.get();

        if (rating < 1 || rating > 10) {
            throw new IllegalArgumentException("Rating must be between 1 and 10");
        }

        var currentAverage = club.getAverageRating();
        var updatedAverage = (currentAverage + rating) / 2;

        club.setAverageRating(updatedAverage);
        clubRepository.save(club);
    }

    @Transactional
    public void addSubscriber(Long clubId, User user) {
        var clubOpt = getClubById(clubId);
        if (clubOpt.isEmpty()) {
            log.warn("Клуб или пользователь не найден: clubId={}, userId={}", clubId, user.getTelegramId());
            return;
        }
        var club = clubOpt.get();
        if (club.getSubscribers().add(user)) {
            log.info("Пользователь {} добавлен в подписчики клуба {}", user.getUsername(), club.getName());
        } else {
            log.warn("Пользователь {} уже подписан на клуб {}", user.getUsername(), club.getName());
        }
    }

    public void removeSubscriber(Long clubId, User user) {
        var clubOpt = getClubById(clubId);
        if (clubOpt.isEmpty()) {
            log.warn("Клуб или пользователь не найден: clubId={}, userId={}", clubId, user.getTelegramId());
            return;
        }

        var club = clubOpt.get();
        if (club.getSubscribers().remove(user)) {
            log.info("Пользователь {} удалён из подписчиков клуба {}", user.getUsername(), club.getName());
        } else {
            log.warn("Пользователь {} не подписан на клуб {}", user.getUsername(), club.getName());
        }
    }

    public List<User> getSubscribers(Long clubId) {
        return getClubById(clubId).map(club -> {
            log.info("Получение списка подписчиков клуба {}", club.getName());
            return new ArrayList<>(club.getSubscribers());
        }).orElse(new ArrayList<>());
    }

    @Transactional
    public boolean deleteById(long id) {
        var clubOpt = getClubById(id);
        if (clubOpt.isPresent()) {
            log.info("Удаление клуба с id {}", id);
            clubRepository.deleteById(id);
            return true;
        }
        log.warn("Клуб с id {} не найден", id);
        return false;
    }
}


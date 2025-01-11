package ru.mkilord.node.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mkilord.node.model.Club;
import ru.mkilord.node.model.User;
import ru.mkilord.node.repository.ClubRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClubService {

    ClubRepository clubRepository;
    UserService userService;

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

    public Optional<Club> update(long id, Club clubDetails) {
        return getClubById(id).map(club -> {
            club.setName(clubDetails.getName());
            club.setDescription(clubDetails.getDescription());
            log.info("Обновление клуба с id {}: {}", id, clubDetails.getName());
            return clubRepository.save(club);
        });
    }

    public Optional<Club> addSubscriber(Long clubId, Long userId) {
        var clubOpt = getClubById(clubId);
        var userOpt = userService.getUserById(userId);

        if (clubOpt.isEmpty() || userOpt.isEmpty()) {
            log.warn("Клуб или пользователь не найден: clubId={}, userId={}", clubId, userId);
            return Optional.empty();
        }

        var club = clubOpt.get();
        var user = userOpt.get();
        if (club.getSubscribers().add(user)) {
            log.info("Пользователь {} добавлен в подписчики клуба {}", user.getUsername(), club.getName());
            return Optional.of(clubRepository.save(club));
        } else {
            log.warn("Пользователь {} уже подписан на клуб {}", user.getUsername(), club.getName());
            return Optional.empty();
        }
    }

    public Optional<Club> removeSubscriber(Long clubId, Long userId) {
        var clubOpt = getClubById(clubId);
        var userOpt = userService.getUserById(userId);

        if (clubOpt.isEmpty() || userOpt.isEmpty()) {
            log.warn("Клуб или пользователь не найден: clubId={}, userId={}", clubId, userId);
            return Optional.empty();
        }

        var club = clubOpt.get();
        var user = userOpt.get();
        if (club.getSubscribers().remove(user)) {
            log.info("Пользователь {} удалён из подписчиков клуба {}", user.getUsername(), club.getName());
            return Optional.of(clubRepository.save(club));
        } else {
            log.warn("Пользователь {} не подписан на клуб {}", user.getUsername(), club.getName());
            return Optional.empty();
        }
    }

    public Optional<Set<User>> getSubscribers(Long clubId) {
        return getClubById(clubId).map(club -> {
            log.info("Получение списка подписчиков клуба {}", club.getName());
            return club.getSubscribers();
        });
    }

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


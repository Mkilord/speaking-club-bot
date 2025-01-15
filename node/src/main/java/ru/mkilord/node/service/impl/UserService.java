package ru.mkilord.node.service.impl;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mkilord.node.model.Meet;
import ru.mkilord.node.model.User;
import ru.mkilord.node.model.enums.MeetStatus;
import ru.mkilord.node.model.enums.Role;
import ru.mkilord.node.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static lombok.AccessLevel.PRIVATE;

@Service
@FieldDefaults(level = PRIVATE, makeFinal = true)
@AllArgsConstructor
public class UserService {

    UserRepository userRepository;

    public List<User> getAll() {
        return userRepository.findAll();
    }

    @Transactional
    public boolean isUserSubscribeToClub(Long userId, Long clubId) {
        return getUserById(userId).map(user -> user.getClubs().stream()
                .anyMatch(club -> clubId.equals(club.getId()))).orElse(false);
    }

    public Optional<User> getUserByNickname(String nickname) {
        return userRepository.findUserByUsername(nickname);
    }

    public List<User> getUsersByRole(Role role) {
        return userRepository.findUsersByRole(role);
    }

    @Transactional
    public List<Meet> getRegisteredMeetsWithStatus(Long userId, MeetStatus status) {
        var userOpt = getUserById(userId);
        if (userOpt.isEmpty()) return new ArrayList<>();
        var user = userOpt.get();
        return user.getMeets().stream().filter(meet -> meet.getStatus() == status).toList();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User save(@Valid User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void deleteById(Long id) {
        var userOpt = getUserById(id);
        if (userOpt.isEmpty()) {
            return;
        }
        var user = userOpt.get();
        var clubs = user.getClubs();
        clubs.forEach(club -> club.getSubscribers().remove(user));
        user.getClubs().clear();
        var meets = user.getMeets();
        meets.forEach(meet -> meet.getRegisteredUsers().remove(user));
        user.getMeets().clear();
        userRepository.delete(user);
    }

    @Transactional
    public Optional<User> grantRole(Long userId, Role role) {
        return getUserById(userId).map(user -> {
            user.setRole(role);
            return user;
        });
    }

    public Optional<User> update(@Valid User user) {
        if (userRepository.existsById(user.getTelegramId())) {
            return Optional.of(userRepository.save(user));
        }
        return Optional.empty();
    }
}

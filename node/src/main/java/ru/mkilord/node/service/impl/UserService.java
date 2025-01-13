package ru.mkilord.node.service.impl;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mkilord.node.model.User;
import ru.mkilord.node.model.enums.Role;
import ru.mkilord.node.repository.UserRepository;

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

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User save(@Valid User user) {
        return userRepository.save(user);
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
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

package ru.mkilord.node.service;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import ru.mkilord.node.model.Role;
import ru.mkilord.node.model.User;
import ru.mkilord.node.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static lombok.AccessLevel.PRIVATE;

@Service
@FieldDefaults(level = PRIVATE)
@AllArgsConstructor
public class UserService {

    UserRepository userRepository;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public User save(@Valid User user) {
        return userRepository.save(user);
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }
    public User grantRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with id " + userId + " does not exist"));
        user.setRole(role);
        return userRepository.save(user);
    }

    public User update(@Valid User user) {
        if (userRepository.existsById(user.getTelegramId())) {
            return userRepository.save(user);
        } else {
            throw new IllegalArgumentException("User with id " + user.getTelegramId() + " does not exist");
        }
    }
}

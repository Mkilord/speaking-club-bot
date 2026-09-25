package ru.mkilord.node.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mkilord.node.config.BotProperties;
import ru.mkilord.node.dialog.DialogStateRepository;
import ru.mkilord.node.model.Role;
import ru.mkilord.node.model.User;
import ru.mkilord.node.repository.UserRepository;
import ru.mkilord.node.util.Validators.FullName;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DialogStateRepository dialogStateRepository;
    private final BotProperties properties;

    /** Finds the user by Telegram id or creates a new one, and keeps username and chat id up to date. */
    public User syncFromTelegram(org.telegram.telegrambots.meta.api.objects.User from, long chatId) {
        var user = userRepository.findById(from.getId())
                .orElseGet(() -> userRepository.save(new User(from.getId(), chatId, from.getUserName())));
        if (!Objects.equals(user.getUsername(), from.getUserName())) {
            user.setUsername(from.getUserName());
        }
        if (!Objects.equals(user.getChatId(), chatId)) {
            user.setChatId(chatId);
        }
        return user;
    }

    public void register(User user, Profile profile) {
        updateProfile(user, profile);
        if (user.getRole() == Role.USER) {
            user.setRole(properties.moderatorIds().contains(user.getTelegramId()) ? Role.MODERATOR : Role.MEMBER);
            log.info("User {} registered as {}", user.getTelegramId(), user.getRole());
        }
    }

    public void updateProfile(User user, Profile profile) {
        user.setLastName(profile.name().lastName());
        user.setFirstName(profile.name().firstName());
        user.setMiddleName(profile.name().middleName());
        user.setPhone(profile.phone());
        user.setEmail(profile.email());
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(long telegramId) {
        return userRepository.findById(telegramId);
    }

    /** Accepts "name" and "@name". */
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        var clean = username.strip();
        if (clean.startsWith("@")) {
            clean = clean.substring(1);
        }
        return userRepository.findFirstByUsernameIgnoreCase(clean);
    }

    @Transactional(readOnly = true)
    public List<User> findByRole(Role role) {
        return userRepository.findByRoleOrderByLastNameAsc(role);
    }

    /** Changes the role and drops the user's current dialog: it may use commands of the old role. */
    public void changeRole(User user, Role role) {
        user.setRole(role);
        dialogStateRepository.resetDialog(user.getChatId());
        log.info("User {} got role {}", user.getTelegramId(), role);
    }

    /** Deletes personal data. Subscriptions, registrations and ratings are removed by the database. */
    public void delete(User user) {
        userRepository.delete(user);
        userRepository.flush();
        log.info("User {} deleted", user.getTelegramId());
    }

    public record Profile(FullName name, String phone, String email) {
    }
}

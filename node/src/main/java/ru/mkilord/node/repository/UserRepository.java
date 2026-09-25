package ru.mkilord.node.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mkilord.node.model.Role;
import ru.mkilord.node.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByRoleOrderByLastNameAsc(Role role);

    Optional<User> findFirstByUsernameIgnoreCase(String username);
}

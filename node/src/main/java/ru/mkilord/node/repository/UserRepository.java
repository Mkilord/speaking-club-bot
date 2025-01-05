package ru.mkilord.node.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mkilord.node.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
}

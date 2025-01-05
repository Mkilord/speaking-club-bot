package ru.mkilord.node.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mkilord.node.model.Club;

public interface ClubRepository extends JpaRepository<Club, Long> {
}

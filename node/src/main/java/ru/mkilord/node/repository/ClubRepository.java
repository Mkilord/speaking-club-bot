package ru.mkilord.node.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mkilord.node.model.Club;

import java.util.List;

public interface ClubRepository extends JpaRepository<Club, Long> {
    List<Club> findAllByOrderByNameAsc();

    boolean existsByIdAndSubscribersTelegramId(Long clubId, Long userId);
}

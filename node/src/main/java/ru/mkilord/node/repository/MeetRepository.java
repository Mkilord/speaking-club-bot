package ru.mkilord.node.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mkilord.node.model.Meet;
import ru.mkilord.node.model.enums.MeetStatus;

import java.util.List;
import java.util.Set;

public interface MeetRepository extends JpaRepository<Meet, Long> {
    List<Meet> findMeetsByClubIdAndStatus(long clubId, MeetStatus status);

    List<Meet> findMeetsByClubIdAndStatusIn(long clubId, Set<MeetStatus> statuses);

    @Query("SELECT m FROM Meet m JOIN m.registeredUsers u WHERE u.telegramId = :telegramId AND m.status = :status")
    List<Meet> findAllByUserTelegramIdAndStatus(@Param("telegramId") Long telegramId, @Param("status") MeetStatus status);
}

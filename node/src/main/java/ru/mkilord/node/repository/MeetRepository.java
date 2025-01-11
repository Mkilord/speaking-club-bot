package ru.mkilord.node.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mkilord.node.model.Meet;
import ru.mkilord.node.model.enums.MeetStatus;

import java.util.List;

public interface MeetRepository extends JpaRepository<Meet, Long> {
    List<Meet> findMeetsByClubIdAndStatus(long clubId, MeetStatus status);

    List<Meet> findMeetsByClubId(long clubId);
}

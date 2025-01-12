package ru.mkilord.node.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mkilord.node.model.Meet;
import ru.mkilord.node.model.enums.MeetStatus;

import java.util.List;
import java.util.Set;

public interface MeetRepository extends JpaRepository<Meet, Long> {
    List<Meet> findMeetsByClubIdAndStatus(long clubId, MeetStatus status);
    List<Meet> findMeetsByClubIdAndStatusIn(long clubId, Set<MeetStatus> statuses);
    List<Meet> findMeetsByClubId(long clubId);
}

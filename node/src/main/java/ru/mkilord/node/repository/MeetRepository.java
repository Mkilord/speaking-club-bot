package ru.mkilord.node.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mkilord.node.model.Meet;
import ru.mkilord.node.model.MeetStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface MeetRepository extends JpaRepository<Meet, Long> {

    List<Meet> findByClubIdAndStatusAndDateGreaterThanEqualOrderByDateAscTimeAsc(
            Long clubId, MeetStatus status, LocalDate from);

    List<Meet> findByClubIdAndStatusInOrderByDateAscTimeAsc(Long clubId, Collection<MeetStatus> statuses);

    @Query("""
            select m from Meet m join m.participants p
            where p.telegramId = :userId and m.status = :status and m.date >= :from
            order by m.date, m.time
            """)
    List<Meet> findForParticipant(@Param("userId") Long userId,
                                  @Param("status") MeetStatus status,
                                  @Param("from") LocalDate from);
}

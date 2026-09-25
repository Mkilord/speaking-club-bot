package ru.mkilord.node.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mkilord.node.model.Club;
import ru.mkilord.node.model.Meet;
import ru.mkilord.node.model.MeetStatus;
import ru.mkilord.node.model.User;
import ru.mkilord.node.repository.MeetRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Meeting life cycle:
 * <pre>
 * HIDDEN --publish--> PUBLISHED --complete--> COMPLETED
 *   |                     |
 * delete               cancel --> CANCELLED
 * </pre>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MeetService {

    private final MeetRepository meetRepository;
    private final Clock clock;

    public Meet create(Club club, String name, LocalDate date, LocalTime time) {
        var meet = meetRepository.save(new Meet(club, name, date, time));
        log.info("Meet {} created in club {}", meet, club);
        return meet;
    }

    @Transactional(readOnly = true)
    public Optional<Meet> find(long id) {
        return meetRepository.findById(id);
    }

    /** Published meetings from today on: what members can sign up for. */
    @Transactional(readOnly = true)
    public List<Meet> findOpen(long clubId) {
        return meetRepository.findByClubIdAndStatusAndDateGreaterThanEqualOrderByDateAscTimeAsc(
                clubId, MeetStatus.PUBLISHED, today());
    }

    /** Hidden and published meetings: what organizers work with. */
    @Transactional(readOnly = true)
    public List<Meet> findManageable(long clubId) {
        return meetRepository.findByClubIdAndStatusInOrderByDateAscTimeAsc(
                clubId, EnumSet.of(MeetStatus.HIDDEN, MeetStatus.PUBLISHED));
    }

    @Transactional(readOnly = true)
    public List<Meet> findUpcomingFor(User user) {
        return meetRepository.findForParticipant(user.getTelegramId(), MeetStatus.PUBLISHED, today());
    }

    public Optional<Meet> publish(long id) {
        return changeStatus(id, MeetStatus.HIDDEN, MeetStatus.PUBLISHED);
    }

    public Optional<Meet> complete(long id) {
        return changeStatus(id, MeetStatus.PUBLISHED, MeetStatus.COMPLETED);
    }

    /** Cancelled meetings are kept, so participants still see what happened. */
    public Optional<Meet> cancel(long id) {
        return changeStatus(id, MeetStatus.PUBLISHED, MeetStatus.CANCELLED);
    }

    /** Only hidden meetings can be deleted: nobody knows about them yet. */
    public boolean delete(long id) {
        var meet = find(id).filter(m -> m.getStatus() == MeetStatus.HIDDEN);
        meet.ifPresent(m -> {
            meetRepository.delete(m);
            log.info("Meet {} deleted", m);
        });
        return meet.isPresent();
    }

    public SignUpResult signUp(long meetId, User user) {
        var meet = find(meetId).filter(m -> m.getStatus() == MeetStatus.PUBLISHED && !m.getDate().isBefore(today()));
        if (meet.isEmpty()) {
            return SignUpResult.NOT_AVAILABLE;
        }
        return meet.get().getParticipants().add(user) ? SignUpResult.SIGNED_UP : SignUpResult.ALREADY_SIGNED_UP;
    }

    public boolean cancelSignUp(long meetId, User user) {
        return find(meetId).map(meet -> meet.getParticipants().remove(user)).orElse(false);
    }

    private Optional<Meet> changeStatus(long id, MeetStatus from, MeetStatus to) {
        return find(id).filter(meet -> meet.getStatus() == from).map(meet -> {
            meet.setStatus(to);
            log.info("Meet {} status {} -> {}", meet, from, to);
            return meet;
        });
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    public enum SignUpResult {
        SIGNED_UP, ALREADY_SIGNED_UP, NOT_AVAILABLE
    }
}

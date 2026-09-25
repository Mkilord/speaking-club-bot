package ru.mkilord.node.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mkilord.node.model.Club;
import ru.mkilord.node.model.ClubRating;
import ru.mkilord.node.model.User;
import ru.mkilord.node.repository.ClubRatingRepository;
import ru.mkilord.node.repository.ClubRepository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ClubService {

    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 10;

    private final ClubRepository clubRepository;
    private final ClubRatingRepository ratingRepository;

    @Transactional(readOnly = true)
    public List<Club> findAll() {
        return clubRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Club> find(long id) {
        return clubRepository.findById(id);
    }

    public Club create(String name, String description) {
        var club = clubRepository.save(new Club(name, description));
        log.info("Club {} created", club);
        return club;
    }

    public Optional<Club> update(long id, String name, String description) {
        return find(id).map(club -> {
            club.setName(name);
            club.setDescription(description);
            log.info("Club {} updated", club);
            return club;
        });
    }

    /** Meetings, registrations and ratings of the club are removed by the database. */
    public boolean delete(long id) {
        if (!clubRepository.existsById(id)) {
            return false;
        }
        clubRepository.deleteById(id);
        clubRepository.flush();
        log.info("Club {} deleted", id);
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isSubscribed(long clubId, User user) {
        return clubRepository.existsByIdAndSubscribersTelegramId(clubId, user.getTelegramId());
    }

    public boolean subscribe(long clubId, User user) {
        return find(clubId).map(club -> club.getSubscribers().add(user)).orElse(false);
    }

    public boolean unsubscribe(long clubId, User user) {
        return find(clubId).map(club -> club.getSubscribers().remove(user)).orElse(false);
    }

    /** Saves the user's rating of the club. A repeated vote replaces the previous one. */
    public void rate(long clubId, User user, int rating) {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new IllegalArgumentException("Rating must be between %d and %d".formatted(MIN_RATING, MAX_RATING));
        }
        var id = new ClubRating.Id(clubId, user.getTelegramId());
        var entity = ratingRepository.findById(id).orElseGet(() -> new ClubRating(clubId, user.getTelegramId(), rating));
        entity.setRating(rating);
        ratingRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public Rating rating(long clubId) {
        var average = ratingRepository.averageForClub(clubId);
        return new Rating(average == null ? 0 : average, ratingRepository.countForClub(clubId));
    }

    public record Rating(double average, long votes) {
        public String format() {
            return votes == 0 ? "пока нет оценок" : "%.1f из %d (оценок: %d)".formatted(average, MAX_RATING, votes);
        }
    }
}

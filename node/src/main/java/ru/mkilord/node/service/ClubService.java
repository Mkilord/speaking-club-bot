package ru.mkilord.node.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mkilord.node.model.Club;
import ru.mkilord.node.model.User;
import ru.mkilord.node.repository.ClubRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClubService {

    ClubRepository clubRepository;
    UserService userService;

    public List<Club> findAll() {
        return clubRepository.findAll();
    }

    public Optional<Club> findById(long id) {
        return clubRepository.findById(id);
    }

    public Club save(Club club) {
        return clubRepository.save(club);
    }

    public Club update(long id, Club clubDetails) {
        return clubRepository.findById(id).map(club -> {
            club.setName(clubDetails.getName());
            club.setDescription(clubDetails.getDescription());
            return clubRepository.save(club);
        }).orElseThrow(() -> new RuntimeException("Club not found with id " + id));
    }


    @Transactional
    public void addSubscriber(Long clubId, Long userId) {
        var club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Club not found"));
        var user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        club.getSubscribers().add(user);
        clubRepository.save(club);
    }

    @Transactional
    public void removeSubscriber(Long clubId, Long userId) {
        var club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Club not found"));
        var user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        club.getSubscribers().remove(user);
        clubRepository.save(club);
    }

    public Set<User> getSubscribers(Long clubId) {
        var club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Club not found"));
        return club.getSubscribers();
    }

    public void deleteById(long id) {
        clubRepository.deleteById(id);
    }
}

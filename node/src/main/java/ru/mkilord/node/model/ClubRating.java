package ru.mkilord.node.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** One rating per user and club. A new vote replaces the previous one. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class ClubRating {

    @EmbeddedId
    private Id id;

    @Column(nullable = false)
    private int rating;

    public ClubRating(long clubId, long userId, int rating) {
        this.id = new Id(clubId, userId);
        this.rating = rating;
    }

    @Embeddable
    @Getter
    @NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.EqualsAndHashCode
    public static class Id implements Serializable {
        @Column(name = "club_id")
        private Long clubId;
        @Column(name = "user_id")
        private Long userId;
    }
}

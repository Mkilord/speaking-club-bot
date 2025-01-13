package ru.mkilord.node.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.mkilord.node.model.enums.MeetStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static lombok.AccessLevel.PRIVATE;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
@Builder
public class Meet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @NotBlank
    String name;

    @NotBlank
    LocalDate date;
    @NotBlank
    LocalTime time;

    @ManyToMany
    @JoinTable(
            name = "meet_user",
            joinColumns = @JoinColumn(name = "meet_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @ToString.Exclude
    Set<User> registeredUsers = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "club_id")
    @ToString.Exclude
    Club club;

    @Enumerated(EnumType.STRING)
    MeetStatus status;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Meet meet = (Meet) o;
        return id == meet.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

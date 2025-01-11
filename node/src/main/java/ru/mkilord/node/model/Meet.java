package ru.mkilord.node.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    String name;

    LocalDate date;
    LocalTime time;

    @ManyToMany
    @JoinTable(
            name = "meet_user",
            joinColumns = @JoinColumn(name = "meet_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    Set<User> registeredUsers = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "club_id")
    Club club;

    @Enumerated(EnumType.STRING)
    MeetStatus status;

    public String getInfo() {
        var croppedName = name;
        if (name.length() > 30) {
            croppedName = name.substring(0, 30) + "...";
        }
        return "%s\n%s %s".formatted(croppedName, date, time);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Meet meet = (Meet) o;
        return id == meet.id && Objects.equals(name, meet.name) && Objects.equals(date, meet.date) && Objects.equals(time, meet.time) && Objects.equals(club, meet.club) && status == meet.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, date, time, club, status);
    }
}

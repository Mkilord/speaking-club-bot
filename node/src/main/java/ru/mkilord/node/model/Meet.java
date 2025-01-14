package ru.mkilord.node.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.mkilord.node.model.enums.MeetStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static lombok.AccessLevel.PRIVATE;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Meet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    long id;

    @NotBlank
    String name;

    LocalDate date;
    LocalTime time;

    @ManyToMany
    @JoinTable(
            name = "meet_user",
            joinColumns = @JoinColumn(name = "meet_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @ToString.Exclude
    Set<User> registeredUsers;

    @ManyToOne
    @JoinColumn(name = "club_id")
    @ToString.Exclude
    Club club;

    @Enumerated(EnumType.STRING)
    MeetStatus status;
}

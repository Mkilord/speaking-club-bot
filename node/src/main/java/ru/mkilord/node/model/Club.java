package ru.mkilord.node.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

import static lombok.AccessLevel.PRIVATE;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Club {
    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @NotBlank
    String description;

    @NotBlank
    String name;

    @ManyToMany
    @JoinTable(
            name = "club_user",
            joinColumns = @JoinColumn(name = "club_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @ToString.Exclude
    Set<User> subscribers;

    @ToString.Exclude
    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<Meet> meets;

    @Column(nullable = false)
    double averageRating;
}

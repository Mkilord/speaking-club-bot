package ru.mkilord.node.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.mkilord.node.model.enums.Role;

import java.util.Set;

import static lombok.AccessLevel.PRIVATE;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldDefaults(level = PRIVATE)
public class User {

    @Id
    @EqualsAndHashCode.Include
    Long telegramId;

    Long chatId;

    String username;

    @Enumerated(EnumType.STRING)
    Role role = Role.USER;

    String firstName;

    String lastName;

    String middleName;

    @Column(unique = true)
    String email;

    @Column(unique = true)
    String phone;

    @ManyToMany(mappedBy = "registeredUsers")
    @ToString.Exclude
    Set<Meet> meets;

    @ManyToMany(mappedBy = "subscribers")
    Set<Club> clubs;
}

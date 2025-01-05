package ru.mkilord.node.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

import static lombok.AccessLevel.PRIVATE;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class User {

    @Id
    Long telegramId;
    String username;

    Long chatId;

    @Enumerated(EnumType.STRING)
    Role role = Role.USER;

    String firstName;

    String lastName;

    String middleName;

    @Column(unique = true)
    String email;

    @Column(unique = true)
    String phone;

    @ManyToMany(mappedBy = "subscribers")
    private Set<Club> clubs = new HashSet<>();
}

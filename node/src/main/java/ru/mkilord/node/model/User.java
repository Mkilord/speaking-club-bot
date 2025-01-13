package ru.mkilord.node.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import ru.mkilord.node.model.enums.Role;

import java.util.HashSet;
import java.util.Objects;
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

    @ToString.Exclude
    @ManyToMany(mappedBy = "subscribers")
    Set<Club> clubs = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(telegramId, user.telegramId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(telegramId);
    }
}

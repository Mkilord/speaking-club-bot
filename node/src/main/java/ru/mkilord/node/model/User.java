package ru.mkilord.node.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    @Id
    @ToString.Include
    @EqualsAndHashCode.Include
    private Long telegramId;

    @Column(nullable = false)
    private Long chatId;

    /** Telegram username without "@". Optional in Telegram, so it can be null. */
    @ToString.Include
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    private String firstName;
    private String lastName;
    private String middleName;
    private String email;
    private String phone;

    public User(Long telegramId, Long chatId, String username) {
        this.telegramId = telegramId;
        this.chatId = chatId;
        this.username = username;
    }

    public String getFullName() {
        return java.util.stream.Stream.of(lastName, firstName, middleName)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.joining(" "));
    }

    public boolean hasRole(java.util.Set<Role> roles) {
        return roles.contains(role);
    }
}

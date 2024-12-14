package ru.mkilord.node.common;

import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@FieldDefaults(level = PRIVATE, makeFinal = true)
public enum UserRole implements Role {
    USER("User"),
    MEMBER("Member"),
    ORGANIZER("Organizer"),
    MODERATOR("Moderator"),
    ADMIN("Admin");

    String name;

    UserRole(String name) {
        this.name = name;
    }

    @Override
    public String get() {
        return name;
    }
}

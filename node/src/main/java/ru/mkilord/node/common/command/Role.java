package ru.mkilord.node.common.command;

import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@FieldDefaults(level = PRIVATE, makeFinal = true)
public enum Role {
    USER(),
    MEMBER(),
    ORGANIZER(),
    MODERATOR(),
    ADMIN();

    public static final Role[] EMPLOYEES = new Role[]{
            MEMBER, ORGANIZER, MODERATOR, ADMIN
    };
    public static final Role[] ALL = new Role[]{USER, MEMBER, ORGANIZER, MODERATOR, ADMIN};
    public static final Role[] MEMBER_EMPLOYEES = new Role[]{
            MEMBER, ORGANIZER, MODERATOR, ADMIN
    };

}

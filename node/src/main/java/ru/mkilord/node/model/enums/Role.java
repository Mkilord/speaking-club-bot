package ru.mkilord.node.model.enums;

import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@FieldDefaults(level = PRIVATE, makeFinal = true)
public enum Role {
    USER(),
    MEMBER(),
    ORGANIZER(),
    MODERATOR();

    public static final Role[] EMPLOYEES = new Role[]{ORGANIZER, MODERATOR};

    public static final Role[] ALL = new Role[]{USER, MEMBER, ORGANIZER, MODERATOR};

    public static final Role[] MEMBER_AND_EMPLOYEES = new Role[]{MEMBER, ORGANIZER, MODERATOR};

    public static final Role[] TEST = MEMBER_AND_EMPLOYEES;
}

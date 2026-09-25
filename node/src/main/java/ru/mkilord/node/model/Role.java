package ru.mkilord.node.model;

import java.util.EnumSet;
import java.util.Set;

public enum Role {
    /** Wrote to the bot but has not registered yet. */
    USER,
    /** Registered member: subscribes to clubs and signs up for meetings. */
    MEMBER,
    /** Creates and runs meetings. */
    ORGANIZER,
    /** Manages clubs and employees. */
    MODERATOR;

    public static final Set<Role> ALL = EnumSet.allOf(Role.class);
    public static final Set<Role> REGISTERED = EnumSet.of(MEMBER, ORGANIZER, MODERATOR);
    public static final Set<Role> EMPLOYEES = EnumSet.of(ORGANIZER, MODERATOR);
}

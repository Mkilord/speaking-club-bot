package ru.mkilord.node.model.enums;

import java.util.Set;

public enum MeetStatus {
    PUBLISHED,
    HIDDEN,
    CANCELLED,
    COMPLETED;

    public static final Set<MeetStatus> OLD_MEETS = Set.of(CANCELLED, COMPLETED);
    public static final Set<MeetStatus> ACTUAL_MEETS = Set.of(PUBLISHED, HIDDEN);
}

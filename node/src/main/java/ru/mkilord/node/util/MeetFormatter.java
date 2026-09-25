package ru.mkilord.node.util;

import ru.mkilord.node.model.Meet;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class MeetFormatter {

    private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd.MM");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("H:mm");

    private MeetFormatter() {
    }

    /** "📆 12.10 🕒 18:30" plus the name on the next line. The year is shown only if it is not the current one. */
    public static String format(Meet meet, LocalDate today) {
        return "📆 %s 🕒 %s\n%s".formatted(date(meet, today), meet.getTime().format(TIME), meet.getName());
    }

    /** Same as {@link #format} with a status icon before the name. */
    public static String formatWithStatus(Meet meet, LocalDate today) {
        var icon = switch (meet.getStatus()) {
            case HIDDEN -> "🙈";
            case PUBLISHED -> "📢";
            case COMPLETED -> "✅";
            case CANCELLED -> "❌";
        };
        return "📆 %s 🕒 %s\n%s %s".formatted(date(meet, today), meet.getTime().format(TIME), icon, meet.getName());
    }

    /** One line for a button: Telegram cuts long button labels anyway. */
    public static String button(Meet meet, LocalDate today) {
        return "%s %s %s".formatted(date(meet, today), meet.getTime().format(TIME), meet.getName());
    }

    private static String date(Meet meet, LocalDate today) {
        return meet.getDate().getYear() == today.getYear()
                ? meet.getDate().format(SHORT_DATE)
                : meet.getDate().format(FULL_DATE);
    }
}

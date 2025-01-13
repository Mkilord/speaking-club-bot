package ru.mkilord.node.util;

import lombok.experimental.FieldDefaults;
import ru.mkilord.node.model.Meet;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static lombok.AccessLevel.PRIVATE;

@FieldDefaults(level = PRIVATE, makeFinal = true)
public class MeetFormatter {
    private static final DateTimeFormatter FULL_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");


    public static String formatMeetWithOutStatus(Meet meet) {
        var currentDate = LocalDate.now();
        var formattedDate = meet.getDate().getYear() == currentDate.getYear()
                ? meet.getDate().format(SHORT_DATE_FORMATTER)
                : meet.getDate().format(FULL_DATE_FORMATTER);
        var formattedTime = meet.getTime().format(TIME_FORMATTER);

        return "\uD83D\uDCC6 %s \uD83D\uDD52 %s\n%s".formatted(formattedDate, formattedTime, meet.getName());
    }

    public static String formatForItemWithStatus(Meet meet) {
        var currentDate = LocalDate.now();
        var formattedDate = meet.getDate().getYear() == currentDate.getYear()
                ? meet.getDate().format(SHORT_DATE_FORMATTER)
                : meet.getDate().format(FULL_DATE_FORMATTER);
        var formattedTime = meet.getTime().format(TIME_FORMATTER);

        var title = switch (meet.getStatus()) {
            case COMPLETED -> "✅ ";
            case PUBLISHED -> "📢 ";
            case HIDDEN -> "🙈 ";
            case CANCELLED -> "❌ ";
        } + meet.getName();

        return "\uD83D\uDCC6 %s \uD83D\uDD52 %s\n%s".formatted(formattedDate, formattedTime, title);
    }
}

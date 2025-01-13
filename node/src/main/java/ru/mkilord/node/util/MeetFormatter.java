package ru.mkilord.node.util;

import ru.mkilord.node.model.Meet;

public class MeetFormatter {
    public static String formatForMessage(Meet meet) {
        return "\uD83D\uDCC6 %s\n\uD83D\uDD52 %s %s".formatted(meet.getDate(), meet.getTime(), meet.getName());
    }

    public static String formatForItem(Meet meet) {
        var croppedName = switch (meet.getStatus()) {
            case COMPLETED -> "✅ ";
            case PUBLISHED -> "📢 ";
            case HIDDEN -> "🙈 ";
            case CANCELLED -> "❌ ";
        } + meet.getName();

        if (meet.getName().length() > 30) {
            croppedName = meet.getName().substring(0, 30) + "...";
        }

        return "\uD83D\uDCC6 %s\n\uD83D\uDD52 %s %s".formatted(meet.getDate(), meet.getTime(), croppedName);
    }
}

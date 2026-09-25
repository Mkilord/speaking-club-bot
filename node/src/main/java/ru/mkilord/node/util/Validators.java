package ru.mkilord.node.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;

/** Parsing and validation of user input. No Spring, easy to unit test. */
public final class Validators {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d.M.uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("H:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private static final Pattern NAME_PART = Pattern.compile("[\\p{L}][\\p{L}'-]{0,49}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final Pattern PHONE = Pattern.compile("\\+?\\d{10,15}");
    private static final Pattern PHONE_NOISE = Pattern.compile("[\\s()-]");
    private static final int MAX_EMAIL_LENGTH = 254;

    private Validators() {
    }

    /** "Иванов Иван Иванович" or "Иванов Иван": last name, first name, optional middle name. */
    public static Optional<FullName> parseFullName(String text) {
        var parts = text.strip().split("\\s+");
        if (parts.length < 2 || parts.length > 3) {
            return Optional.empty();
        }
        for (var part : parts) {
            if (!NAME_PART.matcher(part).matches()) {
                return Optional.empty();
            }
        }
        return Optional.of(new FullName(parts[0], parts[1], parts.length == 3 ? parts[2] : null));
    }

    public static boolean isEmail(String text) {
        return text.length() <= MAX_EMAIL_LENGTH && EMAIL.matcher(text).matches();
    }

    /** Removes spaces, brackets and dashes: "+7 (910) 679-07-83" becomes "+79106790783". */
    public static Optional<String> normalizePhone(String text) {
        var phone = PHONE_NOISE.matcher(text).replaceAll("");
        return PHONE.matcher(phone).matches() ? Optional.of(phone) : Optional.empty();
    }

    public static OptionalInt parseIntInRange(String text, int min, int max) {
        try {
            var value = Integer.parseInt(text.strip());
            return value >= min && value <= max ? OptionalInt.of(value) : OptionalInt.empty();
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    public static Optional<LocalDate> parseDate(String text) {
        try {
            return Optional.of(LocalDate.parse(text.strip(), DATE));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    public static Optional<LocalTime> parseTime(String text) {
        try {
            return Optional.of(LocalTime.parse(text.strip(), TIME));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    public static boolean hasLength(String text, int min, int max) {
        var length = text.strip().length();
        return length >= min && length <= max;
    }

    public record FullName(String lastName, String firstName, String middleName) {
    }
}

package ru.mkilord.node.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class ValidatorsTest {

    @Test
    void parsesFullNameInRussianOrder() {
        var name = Validators.parseFullName("  Иванов   Иван Иванович ").orElseThrow();

        assertThat(name.lastName()).isEqualTo("Иванов");
        assertThat(name.firstName()).isEqualTo("Иван");
        assertThat(name.middleName()).isEqualTo("Иванович");
    }

    @Test
    void middleNameIsOptional() {
        var name = Validators.parseFullName("Smith John").orElseThrow();

        assertThat(name.middleName()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Иван", "Иванов Иван Иванович Лишний", "Иванов 123", "", "Иванов Иван!"})
    void rejectsBadFullNames(String text) {
        assertThat(Validators.parseFullName(text)).isEmpty();
    }

    @Test
    void normalizesPhone() {
        assertThat(Validators.normalizePhone("+7 (910) 679-07-83")).contains("+79106790783");
        assertThat(Validators.normalizePhone("89106790783")).contains("89106790783");
        assertThat(Validators.normalizePhone("12345")).isEmpty();
        assertThat(Validators.normalizePhone("0x12345678901")).isEmpty();
    }

    @Test
    void checksEmail() {
        assertThat(Validators.isEmail("user@example.com")).isTrue();
        assertThat(Validators.isEmail("user@localhost")).isFalse();
        assertThat(Validators.isEmail("user example@mail.ru")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "11", "5.5", "0x5", "1e1", "abc", ""})
    void rejectsRatingOutsideRange(String text) {
        assertThat(Validators.parseIntInRange(text, 1, 10)).isEmpty();
    }

    @Test
    void acceptsRatingInRange() {
        assertThat(Validators.parseIntInRange(" 7 ", 1, 10)).hasValue(7);
    }

    @Test
    void parsesDateStrictly() {
        assertThat(Validators.parseDate("05.10.2026")).contains(LocalDate.of(2026, 10, 5));
        assertThat(Validators.parseDate("5.10.2026")).contains(LocalDate.of(2026, 10, 5));
        assertThat(Validators.parseDate("31.02.2026")).isEmpty();
        assertThat(Validators.parseDate("2026-10-05")).isEmpty();
    }

    @Test
    void parsesTime() {
        assertThat(Validators.parseTime("9:30")).contains(LocalTime.of(9, 30));
        assertThat(Validators.parseTime("18:05")).contains(LocalTime.of(18, 5));
        assertThat(Validators.parseTime("25:00")).isEmpty();
        assertThat(Validators.parseTime("18.00")).isEmpty();
    }
}

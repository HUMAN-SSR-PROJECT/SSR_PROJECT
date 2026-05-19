package com.ssrpro.library.support;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class BirthDateUtils {

    private static final DateTimeFormatter BASIC = DateTimeFormatter.BASIC_ISO_DATE;

    private BirthDateUtils() {
    }

    public static String formatYyyyMmDd(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(BASIC);
    }

    public static LocalDate parseYyyyMmDd(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() != 8) {
            return null;
        }
        try {
            return LocalDate.parse(digits, BASIC);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    public static String maskEmailLocal(String email) {
        if (email == null || email.isBlank()) {
            return "*****";
        }
        int at = email.indexOf('@');
        String local = at > 0 ? email.substring(0, at) : email;
        if (local.length() <= 5) {
            return "*".repeat(Math.max(local.length(), 1));
        }
        return local.substring(0, 5) + "*****";
    }
}

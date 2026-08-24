package com.example.logparser.modules;

import com.example.logparser.models.LogFile;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utilities {
    public static boolean isNullOrEmpty(Object o) {
        if (o == null) {
            return true;
        }

        if (o instanceof String) {
            return ((String) o).isEmpty();
        }

        if (o instanceof TreeMap) {
            return ((TreeMap<?, ?>) o).isEmpty();
        }

        if (o instanceof StringBuilder) {
            return ((StringBuilder) o).isEmpty();
        }

        if (o instanceof LogFile) {
            return  ((LogFile) o).getFileName().isEmpty();
        }

        if (o instanceof org.apache.lucene.store.Directory) {
            try {
                String[] arr = ((org.apache.lucene.store.Directory) o).listAll();
                if (arr.length > 0) {
                    return true;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return false;
    }

    public static boolean isNull(Object o) {
        return o == null;
    }

    public static String capitalizeFirstLetter(String input) {
        if (isNullOrEmpty(input)) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public static LocalDate convertIntToDate(int days) {
        LocalDate baseDate = LocalDate.of(2000, 1, 1);
        return baseDate.plusDays(days);
    }

    public static boolean containsRegexPattern(String input, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        return matcher.find();
    }

    public static boolean containsDateTime(String input, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

        String[] strings = input.split("[\t ]");

        for (String s : strings) {
            try {
                ZonedDateTime.parse(s.trim(), formatter);
                return true;
            } catch (DateTimeParseException ignored) {}
        }
        return false;
    }

    public static boolean startsWithNumeric(String str) {
        if (isNullOrEmpty(str)) {
            return false;
        }
        return Character.isDigit(str.charAt(0));
    }

    public static String parseDateFromString(String input, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

        String[] strings = input.split("[\t ]");

        for (String s : strings) {
            try {
                ZonedDateTime.parse(s.trim(), formatter);
                return s;
            } catch (DateTimeParseException ignored) {}
        }
        return "";
    }

    public static String extractNumericFromString(String input) {
        return input.replaceAll("[^\\d.]", "");
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static Optional<String> getExtensionByStringHandling(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }
}

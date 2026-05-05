package it.fabioformosa.nplusonequeryproblemdetector.scan;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class SqlFingerprinting {

    private static final Pattern QUOTED_STRING = Pattern.compile("'([^']|'')*'");
    private static final Pattern NUMBER = Pattern.compile("(?<![\\w.])-?\\d+(?:\\.\\d+)?(?![\\w.])");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private SqlFingerprinting() {
        throw new IllegalStateException("Utility class");
    }

    public static String normalize(String sql) {
        String withoutStrings = QUOTED_STRING.matcher(sql).replaceAll("?");
        String withoutNumbers = NUMBER.matcher(withoutStrings).replaceAll("?");
        return WHITESPACE.matcher(withoutNumbers)
                .replaceAll(" ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    public static List<SqlFingerprint> repeatedSelectFingerprints(List<String> sqlStatements) {
        Map<String, Long> counts = sqlStatements.stream()
                .map(SqlFingerprinting::normalize)
                .filter(sql -> sql.startsWith("select"))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(entry -> new SqlFingerprint(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(SqlFingerprint::count).reversed())
                .toList();
    }
}

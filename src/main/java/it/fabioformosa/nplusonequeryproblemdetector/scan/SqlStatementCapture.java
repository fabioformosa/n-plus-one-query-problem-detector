package it.fabioformosa.nplusonequeryproblemdetector.scan;

import java.util.ArrayList;
import java.util.List;

public final class SqlStatementCapture {

    private static final ThreadLocal<List<String>> CURRENT_STATEMENTS = new ThreadLocal<>();

    private SqlStatementCapture() {
        throw new IllegalStateException("Utility class");
    }

    public static void start() {
        CURRENT_STATEMENTS.set(new ArrayList<>());
    }

    public static void record(String sql) {
        List<String> statements = CURRENT_STATEMENTS.get();
        if (statements != null && sql != null) {
            statements.add(sql);
        }
    }

    public static List<String> stop() {
        List<String> statements = CURRENT_STATEMENTS.get();
        CURRENT_STATEMENTS.remove();
        return statements == null ? List.of() : List.copyOf(statements);
    }
}

package it.fabioformosa.nplusonequeryproblemdetector.scan;

public record NPlusOneThresholds(
        long minEntityFetches,
        long minCollectionFetches,
        long minRepeatedSelectFingerprint,
        long minPreparedStatements
) {

    public static NPlusOneThresholds defaults() {
        return new NPlusOneThresholds(2, 2, 2, 10);
    }
}

package it.fabioformosa.nplusonequeryproblemdetector.scan;

import it.fabioformosa.nplusonequeryproblemdetector.engine.HibernateStatsSnapshot;

import java.util.Map;
import java.util.List;

public record NPlusOneDetectionContext(
        NPlusOneTestIdentifier testIdentifier,
        HibernateStatsSnapshot stats,
        Map<String, Long> collectionFetchCounts,
        Map<String, Long> entityFetchCounts,
        List<SqlFingerprint> repeatedSqlFingerprints,
        NPlusOneThresholds thresholds
) {

    public long maxRepeatedSelectFingerprintCount() {
        return repeatedSqlFingerprints.stream()
                .mapToLong(SqlFingerprint::count)
                .max()
                .orElse(0);
    }

    public boolean hasRepeatedSqlEvidence() {
        return maxRepeatedSelectFingerprintCount() >= thresholds.minRepeatedSelectFingerprint();
    }
}

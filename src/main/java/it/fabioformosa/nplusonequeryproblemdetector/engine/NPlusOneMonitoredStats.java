package it.fabioformosa.nplusonequeryproblemdetector.engine;

import java.util.Map;

public record NPlusOneMonitoredStats(
        HibernateStatsSnapshot aggregate,
        Map<String, Long> collectionFetchCounts,
        Map<String, Long> entityFetchCounts
) {
}

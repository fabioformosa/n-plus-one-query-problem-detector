package it.fabioformosa.nplusonequeryproblemdetector.engine;

import java.util.Map;

record HibernateDetailedStatsSnapshot(
        HibernateStatsSnapshot aggregate,
        Map<String, Long> collectionFetchCounts,
        Map<String, Long> entityFetchCounts
) {
}

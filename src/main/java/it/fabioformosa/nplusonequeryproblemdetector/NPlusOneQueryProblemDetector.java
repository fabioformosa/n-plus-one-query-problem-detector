package it.fabioformosa.nplusonequeryproblemdetector;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("java:S6813")
public class NPlusOneQueryProblemDetector {

    @Autowired
    private EntityManager entityManager;

    private HibernateStatsSnapshot startSnapshot;
    private HibernateStatsSnapshot endSnapshot;

    public void startMonitoring() {
        Statistics statistics = getSessionStatistics();
        statistics.clear();
        startSnapshot = takeStatsSnapshot(statistics);
    }

    public void stopMonitoring() {
        Statistics statistics = getSessionStatistics();
        endSnapshot = takeStatsSnapshot(statistics);
        statistics.clear();
    }

    private Statistics getSessionStatistics() {
        Session session = entityManager.unwrap(Session.class);
        return session.getSessionFactory().getStatistics();
    }

    private HibernateStatsSnapshot takeStatsSnapshot(Statistics statistics) {
        return HibernateStatsSnapshot.builder()
                .queryExecutionCount(statistics.getQueryExecutionCount())
                .entityFetchCount(statistics.getEntityFetchCount())
                .collectionFetchCount(statistics.getCollectionFetchCount())
                .secondLevelCacheHitCount(statistics.getSecondLevelCacheHitCount())
                .build();
    }

    public HibernateStatsSnapshot getMonitoredStats() {
        return HibernateStatsSnapshot.builder()
                .queryExecutionCount(endSnapshot.getQueryExecutionCount() - startSnapshot.getQueryExecutionCount())
                .entityFetchCount(endSnapshot.getEntityFetchCount() - startSnapshot.getEntityFetchCount())
                .collectionFetchCount(endSnapshot.getCollectionFetchCount() - startSnapshot.getCollectionFetchCount())
                .secondLevelCacheHitCount(endSnapshot.getSecondLevelCacheHitCount() - startSnapshot.getSecondLevelCacheHitCount())
                .build();
    }
}

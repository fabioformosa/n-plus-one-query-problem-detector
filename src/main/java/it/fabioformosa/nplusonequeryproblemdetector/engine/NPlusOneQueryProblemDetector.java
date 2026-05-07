package it.fabioformosa.nplusonequeryproblemdetector.engine;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@SuppressWarnings("java:S6813")
public class NPlusOneQueryProblemDetector {

    @Autowired(required = false)
    private EntityManager entityManager;

    @Autowired(required = false)
    private EntityManagerFactory entityManagerFactory;

    private HibernateDetailedStatsSnapshot startSnapshot;
    private HibernateDetailedStatsSnapshot endSnapshot;
    private boolean statisticsEnabledBeforeMonitoring;

    @Autowired
    public NPlusOneQueryProblemDetector() {
    }

    public NPlusOneQueryProblemDetector(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public boolean canMonitor() {
        return entityManagerFactory != null || entityManager != null;
    }

    public void startMonitoring() {
        Statistics statistics = getSessionStatistics();
        statisticsEnabledBeforeMonitoring = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        startSnapshot = takeDetailedStatsSnapshot(statistics);
    }

    public void stopMonitoring() {
        Statistics statistics = getSessionStatistics();
        endSnapshot = takeDetailedStatsSnapshot(statistics);
        statistics.setStatisticsEnabled(statisticsEnabledBeforeMonitoring);
    }

    private Statistics getSessionStatistics() {
        if (entityManagerFactory != null) {
            return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        }
        Session session = entityManager.unwrap(Session.class);
        return session.getSessionFactory().getStatistics();
    }

    private HibernateDetailedStatsSnapshot takeDetailedStatsSnapshot(Statistics statistics) {
        return new HibernateDetailedStatsSnapshot(
                HibernateStatsSnapshot.builder()
                        .queryExecutionCount(statistics.getQueryExecutionCount())
                        .prepareStatementCount(statistics.getPrepareStatementCount())
                        .entityFetchCount(statistics.getEntityFetchCount())
                        .collectionFetchCount(statistics.getCollectionFetchCount())
                        .secondLevelCacheHitCount(statistics.getSecondLevelCacheHitCount())
                        .build(),
                Arrays.stream(statistics.getCollectionRoleNames())
                        .collect(Collectors.toMap(Function.identity(), role -> statistics.getCollectionStatistics(role).getFetchCount())),
                Arrays.stream(statistics.getEntityNames())
                        .collect(Collectors.toMap(Function.identity(), entity -> statistics.getEntityStatistics(entity).getFetchCount()))
        );
    }

    public HibernateStatsSnapshot getMonitoredStats() {
        return getDetailedMonitoredStats().aggregate();
    }

    public NPlusOneMonitoredStats getDetailedMonitoredStats() {
        HibernateStatsSnapshot startAggregate = startSnapshot.aggregate();
        HibernateStatsSnapshot endAggregate = endSnapshot.aggregate();
        HibernateStatsSnapshot aggregateDelta = HibernateStatsSnapshot.builder()
                .queryExecutionCount(endAggregate.getQueryExecutionCount() - startAggregate.getQueryExecutionCount())
                .prepareStatementCount(endAggregate.getPrepareStatementCount() - startAggregate.getPrepareStatementCount())
                .entityFetchCount(endAggregate.getEntityFetchCount() - startAggregate.getEntityFetchCount())
                .collectionFetchCount(endAggregate.getCollectionFetchCount() - startAggregate.getCollectionFetchCount())
                .secondLevelCacheHitCount(endAggregate.getSecondLevelCacheHitCount() - startAggregate.getSecondLevelCacheHitCount())
                .build();

        return new NPlusOneMonitoredStats(
                aggregateDelta,
                deltaMap(startSnapshot.collectionFetchCounts(), endSnapshot.collectionFetchCounts()),
                deltaMap(startSnapshot.entityFetchCounts(), endSnapshot.entityFetchCounts())
        );
    }

    private Map<String, Long> deltaMap(Map<String, Long> start, Map<String, Long> end) {
        return end.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue() - start.getOrDefault(entry.getKey(), 0L)));
    }

}

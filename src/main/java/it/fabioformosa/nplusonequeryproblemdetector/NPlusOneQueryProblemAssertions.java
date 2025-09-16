package it.fabioformosa.nplusonequeryproblemdetector;

public class NPlusOneQueryProblemAssertions {

    private NPlusOneQueryProblemAssertions() {
        throw new IllegalStateException("Utility class");
    }

    public static NPlusOneQueryProblemAssert assertThat(NPlusOneQueryProblemDetector detector) {
        return new NPlusOneQueryProblemAssert(detector);
    }

    public static NPlusOneQueryProblemStatsAssert assertThat(HibernateStatsSnapshot stats) {
        return new NPlusOneQueryProblemStatsAssert(stats);
    }

    public static class NPlusOneQueryProblemAssert {
        private final NPlusOneQueryProblemDetector detector;

        public NPlusOneQueryProblemAssert(NPlusOneQueryProblemDetector detector) {
            this.detector = detector;
        }

        public void hasCountedMaxQueries(long maxQueries) {
            // Calculate the number of queries executed during the monitored period
            HibernateStatsSnapshot diffSnapshot = detector.getMonitoredStats();

            long totalExecuted = diffSnapshot.calculateTotalQueries();
            if (totalExecuted > maxQueries) {
                throw new AssertionError(String.format(
                        "Expected maximum %d queries, but %d were executed. Query Execution: %d, Entity fetches: %d, Collection fetches: %d, Second level cache hits: %d",
                        maxQueries, totalExecuted, diffSnapshot.getQueryExecutionCount(),
                        diffSnapshot.getEntityFetchCount(), diffSnapshot.getCollectionFetchCount(),
                        diffSnapshot.getSecondLevelCacheHitCount()));
            }
        }
    }


    public static class NPlusOneQueryProblemStatsAssert {
        private final HibernateStatsSnapshot stats;

        public NPlusOneQueryProblemStatsAssert(HibernateStatsSnapshot stats) {
            this.stats = stats;
        }

        public void queryExecutionCountIsEqualTo(int expectedCount) {
            if (stats.getQueryExecutionCount() != expectedCount) {
                throw new AssertionError(String.format(
                        "Expected query execution count to be %d, but was %d",
                        expectedCount, stats.getQueryExecutionCount()));
            }

        }

        public void collectionFetchCountIsEqualTo(int expectedCount) {
            if (stats.getCollectionFetchCount() != expectedCount) {
                throw new AssertionError(String.format(
                        "Expected collection fetch count to be %d, but was %d",
                        expectedCount, stats.getCollectionFetchCount()));
            }

        }

        public void entityFetchCountIsEqualTo(int expectedCount) {
            if (stats.getEntityFetchCount() != expectedCount) {
                throw new AssertionError(String.format(
                        "Expected entity fetch count to be %d, but was %d",
                        expectedCount, stats.getEntityFetchCount()));
            }
        }
    }
}



package it.fabioformosa.nplusonequeryproblemdetector.engine;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HibernateStatsSnapshot {
    private final long queryExecutionCount;
    private final long prepareStatementCount;
    private final long entityFetchCount;
    private final long collectionFetchCount;
    private final long secondLevelCacheHitCount;

    public long calculateTotalQueries(){
        return queryExecutionCount + entityFetchCount + collectionFetchCount + secondLevelCacheHitCount;
    }
}


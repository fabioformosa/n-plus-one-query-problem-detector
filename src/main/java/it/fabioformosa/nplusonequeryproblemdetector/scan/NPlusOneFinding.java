package it.fabioformosa.nplusonequeryproblemdetector.scan;

import it.fabioformosa.nplusonequeryproblemdetector.engine.HibernateStatsSnapshot;

import java.util.List;

public class NPlusOneFinding {

    private final NPlusOneTestIdentifier testIdentifier;
    private final NPlusOneConfidence confidence;
    private final String reason;
    private final HibernateStatsSnapshot stats;
    private final String associationRole;
    private final String entityName;
    private final List<SqlFingerprint> repeatedSqlFingerprints;
    private boolean excluded;
    private String exclusionReason;

    public NPlusOneFinding(NPlusOneTestIdentifier testIdentifier, NPlusOneConfidence confidence, String reason,
                           HibernateStatsSnapshot stats, String associationRole, String entityName,
                           List<SqlFingerprint> repeatedSqlFingerprints) {
        this.testIdentifier = testIdentifier;
        this.confidence = confidence;
        this.reason = reason;
        this.stats = stats;
        this.associationRole = associationRole;
        this.entityName = entityName;
        this.repeatedSqlFingerprints = repeatedSqlFingerprints;
    }

    public NPlusOneTestIdentifier getTestIdentifier() {
        return testIdentifier;
    }

    public NPlusOneConfidence getConfidence() {
        return confidence;
    }

    public String getReason() {
        return reason;
    }

    public HibernateStatsSnapshot getStats() {
        return stats;
    }

    public String getAssociationRole() {
        return associationRole;
    }

    public String getEntityName() {
        return entityName;
    }

    public List<SqlFingerprint> getRepeatedSqlFingerprints() {
        return repeatedSqlFingerprints;
    }

    public boolean isExcluded() {
        return excluded;
    }

    public String getExclusionReason() {
        return exclusionReason;
    }

    public void exclude(String exclusionReason) {
        this.excluded = true;
        this.exclusionReason = exclusionReason;
    }
}

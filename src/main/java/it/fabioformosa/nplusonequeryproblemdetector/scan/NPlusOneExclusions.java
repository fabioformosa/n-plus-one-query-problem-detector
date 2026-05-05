package it.fabioformosa.nplusonequeryproblemdetector.scan;

import java.util.regex.Pattern;

public class NPlusOneExclusions {

    private final NPlusOneScanProperties properties;

    public NPlusOneExclusions(NPlusOneScanProperties properties) {
        this.properties = properties;
    }

    public void apply(NPlusOneFinding finding) {
        String testDisplayName = finding.getTestIdentifier().displayName();
        if (matchesAnyWildcard(properties.excludedTests(), testDisplayName)) {
            finding.exclude("matched nplusone.scan.excluded-tests");
            return;
        }
        if (finding.getAssociationRole() != null && matchesAnyWildcard(properties.excludedAssociations(), finding.getAssociationRole())) {
            finding.exclude("matched nplusone.scan.excluded-associations");
            return;
        }
        if (finding.getEntityName() != null && matchesAnyWildcard(properties.excludedEntities(), finding.getEntityName())) {
            finding.exclude("matched nplusone.scan.excluded-entities");
            return;
        }
        if (matchesAnySqlPattern(finding)) {
            finding.exclude("matched nplusone.scan.excluded-sql-fingerprint-patterns");
        }
    }

    private boolean matchesAnyWildcard(Iterable<String> patterns, String value) {
        for (String pattern : patterns) {
            if (wildcardToRegex(pattern).matcher(value).matches()) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAnySqlPattern(NPlusOneFinding finding) {
        for (SqlFingerprint fingerprint : finding.getRepeatedSqlFingerprints()) {
            for (String pattern : properties.excludedSqlFingerprintPatterns()) {
                if (Pattern.compile(pattern).matcher(fingerprint.sql()).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    private Pattern wildcardToRegex(String wildcard) {
        String regex = "^" + Pattern.quote(wildcard).replace("*", "\\E.*\\Q") + "$";
        return Pattern.compile(regex);
    }
}

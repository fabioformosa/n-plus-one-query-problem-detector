![https://github.com/fabioformosa/n-plus-one-query-problem-detector/actions/workflows/ci.yml](https://github.com/fabioformosa/n-plus-one-query-problem-detector/actions/workflows/ci.yml/badge.svg) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=fabioformosa_n-plus-one-query-problem-detector&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=fabioformosa_n-plus-one-query-problem-detector)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=fabioformosa_n-plus-one-query-problem-detector&metric=coverage)](https://sonarcloud.io/summary/new_code?id=fabioformosa_n-plus-one-query-problem-detector)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=fabioformosa_n-plus-one-query-problem-detector&metric=bugs)](https://sonarcloud.io/summary/new_code?id=fabioformosa_n-plus-one-query-problem-detector)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=fabioformosa_n-plus-one-query-problem-detector&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=fabioformosa_n-plus-one-query-problem-detector)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=fabioformosa_n-plus-one-query-problem-detector&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=fabioformosa_n-plus-one-query-problem-detector)

# N+1 Query Problem Detector

A Spring Java testing library for detecting the N+1 query problem in Hibernate-based applications.

Compatibility: this library is compatible with Spring Boot 3.5.x and Spring Boot 4.x.

## What's the N+1 Query Problem?

The N+1 query problem is a common performance issue in ORM frameworks like Hibernate. It occurs when an application executes one query to fetch a list of entities (the "1"), and then, for each entity, executes an additional query (the "N") to fetch related data. This can lead to a large number of unnecessary database queries, severely impacting performance.

## Why a N+1 Query Problem Detector?

This library provides utilities to detect and assert the presence of the N+1 query problem in your Spring/Hibernate integration tests. It helps you:

- Monitor Hibernate statistics during test execution
- Assert the number of queries executed
- Fail tests if the N+1 problem is detected

## Get Started

1. **Add the library to your project**
   - When available on Maven Central, add the following dependency to your `pom.xml`:
     ```xml
     <dependency>
       <groupId>it.fabioformosa</groupId>
       <artifactId>n-plus-one-query-problem-detector</artifactId>
       <version>REPLACE_WITH_LATEST_VERSION</version>
       <scope>test</scope>
     </dependency>
     ```
   - For Gradle:
     ```groovy
     testImplementation 'it.fabioformosa:n-plus-one-query-problem-detector:REPLACE_WITH_LATEST_VERSION'
     ```

2. **Choose how to use the library**

   Use **scan mode** when you want to get a report of possible N+1 query candidates about your existing test suite of integration tests, based on Spring Framework, without changing them.

   ```properties
   n-plus-one-query-detector.scan.enabled=true
   ```

   Use **explicit assertion mode** when you want to control regressions on the n+1 query problem, making fail your integration tests when they don't pass assertions about the expected number of performed queries. The JUnit extension starts and stops monitoring around the test method automatically:

   ```java
   import it.fabioformosa.nplusonequeryproblemdetector.junitextension.ExpectMaxQueries;
   import it.fabioformosa.nplusonequeryproblemdetector.junitextension.NPlusOneQueryProblemDetectorExtension;
   import org.junit.jupiter.api.Test;
   import org.junit.jupiter.api.extension.ExtendWith;
   import org.springframework.beans.factory.annotation.Autowired;
   import org.springframework.boot.test.context.SpringBootTest;

   @SpringBootTest
   @ExtendWith(NPlusOneQueryProblemDetectorExtension.class)
   class CompanyServiceTest {

       @Autowired
       private CompanyService companyService;

       @Test
       @ExpectMaxQueries(1)
       void listCompaniesWithoutNPlusOneQueries() {
           companyService.list(0, 5);
       }
   }
   ```

## Testing Library Usage

### JUnit Extension With Annotations

Use the JUnit extension when you want simply decorate the integration tests, avoiding to get injected the Detector Object and write assertions within the test as in the manual mode, described in the next section.

```java
@SpringBootTest
@ExtendWith(NPlusOneQueryProblemDetectorExtension.class)
class CompanyServiceTest {

    @Autowired
    private CompanyService companyService;

    @Test
    @ExpectMaxQueries(1)
    void listCompaniesAndFetchEmployees() {
        companyService.list(0, 5);
    }
}
```

Queries executed in `@BeforeEach` are not counted because the extension starts monitoring immediately before the test method body. Queries executed at the beginning of the test method are counted.

If the test method needs arrangement queries before the business logic under test, restart monitoring after the arrangement:

```java
@Test
@ExpectMaxQueries(1)
void listCompaniesWithoutCountingArrangementQueries() {
    companyRepository.save(CompanyBuilder.build()); // arrangement, initially counted

    NPlusOneQueryMonitoring.restart();

    companyService.list(0, 5); // only this part is asserted
}
```

`NPlusOneQueryMonitoring.restart()` can be used only inside a test executed with `NPlusOneQueryProblemDetectorExtension`.

### Manual Detector Usage

Inject `NPlusOneQueryProblemDetector` when you need full control over the monitoring boundaries or when you prefer explicit assertion calls.

```java
@Autowired
private NPlusOneQueryProblemDetector detector;

@Test
void testNPlusOne() {
    prepareData();

    detector.startMonitoring();
    companyService.list(0, 5);
    detector.stopMonitoring();

    NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(2);
}
```
Please find all assertions you can use in the next sections.

### Scan Mode For Existing Spring Tests

Scan mode observes existing Spring integration tests without adding detector objects, JUnit extensions, or expectation annotations to those tests.

The library contributes a Spring `TestExecutionListener` through `META-INF/spring.factories`. Spring TestContext can discover that listener automatically from the dependency jar. 
The listener is intentionally passive by default, so adding the dependency does not change test behavior unless scan mode is enabled.

Enable scan mode in test properties:

```properties
n-plus-one-query-detector.scan.enabled=true
```

The application tests only need `n-plus-one-query-detector.scan.enabled=true`.

When enabled, the listener wraps every Spring test method, snapshots Hibernate `Statistics` before and after the test, captures SQL fingerprints, classifies possible N+1 candidates, and prints one aggregate console report when the test JVM exits.

Scan mode reports candidates instead of comparing against expected query counts. It detects likely N+1 behavior through lazy fetch counters and repeated SQL shapes.

Confidence levels:

| Confidence | Criteria |
| --- | --- |
| `HIGH` | Lazy collection/entity fetches exceed the threshold and repeated `SELECT` SQL fingerprints confirm the pattern. |
| `MEDIUM` | Lazy collection/entity fetches exceed the threshold, but SQL fingerprints are unavailable, or repeated `SELECT` fingerprints are detected without Hibernate lazy-fetch counters. |
| `LOW` | Prepared statement count exceeds the threshold without a more specific lazy-fetch or repeated-SQL signal. |

Default thresholds:

```properties
n-plus-one-query-detector.scan.threshold.min-entity-fetches=2
n-plus-one-query-detector.scan.threshold.min-collection-fetches=2
n-plus-one-query-detector.scan.threshold.min-repeated-select-fingerprint=2
n-plus-one-query-detector.scan.threshold.min-prepared-statements=10
```

Failing the build is disabled by default:

```properties
n-plus-one-query-detector.scan.fail-on-detected=false
```

To fail only on high-confidence non-excluded findings:

```properties
n-plus-one-query-detector.scan.fail-on-detected=true
n-plus-one-query-detector.scan.fail-on-confidence=HIGH
```

To fail on medium and high confidence findings:

```properties
n-plus-one-query-detector.scan.fail-on-detected=true
n-plus-one-query-detector.scan.fail-on-confidence=MEDIUM
```

False positive exclusions:

```properties
n-plus-one-query-detector.scan.excluded-tests=com.example.LegacyCompanyServiceTest.*
n-plus-one-query-detector.scan.excluded-associations=com.example.Company.employees
n-plus-one-query-detector.scan.excluded-entities=com.example.Company
n-plus-one-query-detector.scan.excluded-sql-fingerprint-patterns=.*from audit_log.*where.*entity_id=\\?.*
```

`n-plus-one-query-detector.scan.excluded-associations` works when Hibernate per-collection-role statistics identify the affected collection role, for example `com.example.Company.employees`. Entity fetch findings can be excluded with `n-plus-one-query-detector.scan.excluded-entities` because Hibernate entity fetch statistics identify the fetched entity type, not always the owning association path.

Report formatting options:

```properties
n-plus-one-query-detector.scan.report.print-sql-fingerprints=true
n-plus-one-query-detector.scan.report.max-sql-fingerprints=5
```

`n-plus-one-query-detector.scan.report.max-sql-fingerprints` limits how many repeated SQL fingerprints are printed per finding. It does not affect detection.

Example report:

```text
================================================================================
N+1 Query Problem Detector - Scan Report
================================================================================

Scan mode: ENABLED
Fail on detected: false
Observed tests: 128
Affected tests: 1
Excluded findings: 0

--------------------------------------------------------------------------------
[HIGH] com.example.CompanyServiceTest.listCompanies
--------------------------------------------------------------------------------
Reason:
  Lazy collection fetch pattern detected and confirmed by repeated SELECT SQL.

Hibernate statistics:
  Query executions:        2
  Prepared statements:     7
  Entity fetches:          0
  Collection fetches:      5
  Second-level cache hits: 0

Likely affected association:
  com.example.Company.employees

Repeated SQL fingerprints:
  5x select e.fk_company,e.id,e.name from employees e where e.fk_company=?

Suggested fixes:
  1. Use JOIN FETCH [RECOMMENDED] when this use case always needs the association.
  2. Use EntityGraph when the fetch shape should be declarative or reusable.
  3. Use batch fetching when lazy loading is acceptable but should be grouped.
  4. Use DTO/projection queries when only selected fields are needed.
```

Scan mode limitations:

- It reports N+1 behavior observed during existing tests. It cannot detect lazy paths that tests do not execute.
- Hibernate `Statistics` are `SessionFactory`-wide, so scan-mode tests should not run in parallel against the same `SessionFactory`.

### Assertions Deep Dive

| Annotation-style | Assertion-style | Description                                                                                                                                                                                                                                                                             |
| --- | --- |-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `@ExpectMaxQueries(max)` | `NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(max)` | Broad query budget for the monitored use case. It fails when total monitored Hibernate stats exceed `max`, including query executions, entity fetches, collection fetches, and second-level cache hits.                                                                                 |
| `@ExpectQueryExecutionCount(count)` | `NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).queryExecutionCountIsEqualTo(count)` | Exact number of executed queries. Useful for repository calls that should run a fixed number of queriesFor instance, a service method which return the first page of a paginate list the expected number of executed queries should be 2 (the fetch query and the total count of items) |
| `@ExpectEntityFetchCount(count)` | `NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).entityFetchCountIsEqualTo(count)` | Exact number of lazy entity fetches. Useful for detecting many-to-one or one-to-one lazy loading patterns. This counter shouldn't be N, whether N is the items of a paginated list                                                                                                      |
| `@ExpectCollectionFetchCount(count)` | `NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).collectionFetchCountIsEqualTo(count)` | Exact number of lazy collection fetches. Useful for detecting one-to-many N+1 patterns when accessing nested collections. This counter shouldn't be N, whether N is the items of a paginated list                                                                                                                                                              |

You can combine annotation-style assertions on the same test when you want both a high-level query budget and precise Hibernate statistic expectations:

```java
@Test
@ExpectMaxQueries(7)
@ExpectQueryExecutionCount(2)
@ExpectCollectionFetchCount(5)
void listCompaniesAndFetchEmployees() {
    companyService.list(0, 5);
}
```

You can use the equivalent assertion-style APIs when the detector is injected manually:

```java
detector.startMonitoring();
companyService.list(0, 5);
detector.stopMonitoring();

NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(7);
NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).queryExecutionCountIsEqualTo(2);
NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).collectionFetchCountIsEqualTo(5);
```

### Caveat: Parallel Test Execution

The detector computes deltas between start and stop Hibernate statistics snapshots, so previous statistics do not affect a sequential monitored test slice.

Detector tests must run in a non-parallel way when they share the same Hibernate `SessionFactory` with other integration tests.

This library relies on Hibernate `Statistics`, and Hibernate statistics belong to the whole `SessionFactory`; they are not scoped to a single test method, thread, transaction, `EntityManager`, or Hibernate `Session`. If another integration test runs at the same time against the same `SessionFactory`, the monitored counters can be polluted by that test's queries. The detector object also keeps its current monitoring window in mutable fields, so overlapping windows that share the same detector bean can overwrite each other.

For this reason, serialization is mandatory for tests that use this detector and share the same `SessionFactory`. Do not run detector tests concurrently with other integration tests that use the same Spring `ApplicationContext` / JPA `EntityManagerFactory` / Hibernate `SessionFactory`.

If your build enables parallel JUnit execution, isolate detector tests by using a non-parallel test group, disabling parallel execution for those tests, or giving them an isolated Spring context / `SessionFactory`.

## Devlog & Video Series
[![Watch the video](https://img.youtube.com/vi/nF8DMHj-fmY/maxresdefault.jpg)](https://www.youtube.com/watch?v=nF8DMHj-fmY)


This project is documented as a devlog, showing how an idea can turn into a project. Follow the development journey on YouTube:

- **YouTube Channel:** [bitaligners](https://www.youtube.com/@bitaligners)
- **Devlog Playlist:** [From Idea to Project: N+1 Query Problem Detector](https://www.youtube.com/watch?v=nF8DMHj-fmY&list=PLIoJ8eJvtC1apnL1jv65VPvr4Q16d04Gc&pp=gAQB)

## License

This project is licensed under the Apache License 2.0.

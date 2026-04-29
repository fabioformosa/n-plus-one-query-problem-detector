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

2. **Write your test with the JUnit extension**
   - This is the simplest way to use the library: annotate the test class with `@ExtendWith(NPlusOneQueryProblemDetectorExtension.class)` and declare expectations on the test method.

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

Use the JUnit extension when you want the detector to start before the test method and stop after the test method automatically. This avoids injecting `NPlusOneQueryProblemDetector` only to bracket the test logic.

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

### Assertions Deep Dive

| Annotation-style | Assertion-style | Description |
| --- | --- | --- |
| `@ExpectMaxQueries(max)` | `NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(max)` | Broad query budget for the monitored use case. It fails when total monitored Hibernate stats exceed `max`, including query executions, entity fetches, collection fetches, and second-level cache hits. |
| `@ExpectQueryExecutionCount(count)` | `NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).queryExecutionCountIsEqualTo(count)` | Exact number of executed queries. Useful for repository calls that should run a fixed number of queries, such as a paginated content query plus count query. |
| `@ExpectEntityFetchCount(count)` | `NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).entityFetchCountIsEqualTo(count)` | Exact number of lazy entity fetches. Useful for detecting many-to-one or one-to-one lazy loading patterns. |
| `@ExpectCollectionFetchCount(count)` | `NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).collectionFetchCountIsEqualTo(count)` | Exact number of lazy collection fetches. Useful for detecting one-to-many N+1 patterns when accessing nested collections. |

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

The library deliberately resets Hibernate statistics when monitoring starts and stops, so the assertions only see the monitored test slice.

Detector tests must run in a non-parallel way when they share the same Hibernate `SessionFactory` with other integration tests.

This library relies on Hibernate `Statistics`, and Hibernate statistics belong to the whole `SessionFactory`; they are not scoped to a single test method, thread, transaction, `EntityManager`, or Hibernate `Session`. When monitoring starts or stops, the detector clears those shared statistics. If another integration test runs at the same time against the same `SessionFactory`, the monitored counters can be polluted by that test's queries, or cleared by another detector test.

For this reason, serialization is mandatory for tests that use this detector and share the same `SessionFactory`. Do not run detector tests concurrently with other integration tests that use the same Spring `ApplicationContext` / JPA `EntityManagerFactory` / Hibernate `SessionFactory`.

If your build enables parallel JUnit execution, isolate detector tests by using a non-parallel test group, disabling parallel execution for those tests, or giving them an isolated Spring context / `SessionFactory`.

## Devlog & Video Series
[![Watch the video](https://img.youtube.com/vi/nF8DMHj-fmY/maxresdefault.jpg)](https://www.youtube.com/watch?v=nF8DMHj-fmY)


This project is documented as a devlog, showing how an idea can turn into a project. Follow the development journey on YouTube:

- **YouTube Channel:** [bitaligners](https://www.youtube.com/@bitaligners)
- **Devlog Playlist:** [From Idea to Project: N+1 Query Problem Detector](https://www.youtube.com/watch?v=nF8DMHj-fmY&list=PLIoJ8eJvtC1apnL1jv65VPvr4Q16d04Gc&pp=gAQB)

## License

This project is licensed under the Apache License 2.0.

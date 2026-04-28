![https://github.com/fabioformosa/n-plus-one-query-problem-detector/actions/workflows/ci.yml](https://github.com/fabioformosa/n-plus-one-query-problem-detector/actions/workflows/ci.yml/badge.svg) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=fabioformosa_n-plus-one-query-problem-detector&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=fabioformosa_n-plus-one-query-problem-detector)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=fabioformosa_n-plus-one-query-problem-detector&metric=coverage)](https://sonarcloud.io/summary/new_code?id=fabioformosa_n-plus-one-query-problem-detector)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=fabioformosa_n-plus-one-query-problem-detector&metric=bugs)](https://sonarcloud.io/summary/new_code?id=fabioformosa_n-plus-one-query-problem-detector)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=fabioformosa_n-plus-one-query-problem-detector&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=fabioformosa_n-plus-one-query-problem-detector)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=fabioformosa_n-plus-one-query-problem-detector&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=fabioformosa_n-plus-one-query-problem-detector)

# N+1 Query Problem Detector

A Spring Java testing library for detecting the N+1 query problem in Hibernate-based applications.

## What's the N+1 Query Problem?

The N+1 query problem is a common performance issue in ORM frameworks like Hibernate. It occurs when an application executes one query to fetch a list of entities (the "1"), and then, for each entity, executes an additional query (the "N") to fetch related data. This can lead to a large number of unnecessary database queries, severely impacting performance.

## Why a N+1 Query Problem Detector?

This library provides utilities to detect and assert the presence of the N+1 query problem in your Spring/Hibernate integration tests. It helps you:

- Monitor Hibernate statistics during test execution
- Assert the number of queries executed
- Fail tests if the N+1 problem is detected

## How to Use

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

2. **Enable Hibernate statistics**
   - In your `application.properties` (test profile):
     ```properties
     spring.jpa.properties.hibernate.generate_statistics=true
     ```

3. **Write your test**
   - Use the JUnit extension and annotations when you want the detector to start and stop automatically around a test method.
   - Inject `NPlusOneQueryProblemDetector` and use explicit assertions when you need to monitor only a smaller block inside the test method.

   Annotation-driven example:
   ```java
   @SpringBootTest
   @ExtendWith(NPlusOneQueryProblemTestDetector.class)
   class CompanyServiceTest {

       @Autowired
       private CompanyService companyService;

       @Test
       @ExpectMaxQueries(5)
       void listCompaniesWithoutNPlusOneQueries() {
           companyService.list(0, 5);
       }
   }
   ```

   Manual example:
   ```java
   @Autowired
   private NPlusOneQueryProblemDetector detector;

   @Test
   void testNPlusOne() {
       detector.startMonitoring();
       // ... your code that may trigger N+1 ...
       detector.stopMonitoring();
        NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(2);
   }
   ```

## Assertions

Use `@ExpectMaxQueries(max)` or `hasCountedMaxQueries(max)` as the broad guard for a use case. It fails when the monitored Hibernate statistics exceed the maximum total query count, including query executions, entity fetches, collection fetches, and second-level cache hits.

Use `@ExpectQueryExecutionCount(count)` or `queryExecutionCountIsEqualTo(count)` when you want the exact number of executed queries, for example a paginated repository call that should run only the content query and count query.

Use `@ExpectEntityFetchCount(count)` or `entityFetchCountIsEqualTo(count)` when you want to detect lazy entity fetches, commonly in many-to-one or one-to-one relationships.

Use `@ExpectCollectionFetchCount(count)` or `collectionFetchCountIsEqualTo(count)` when you want to detect lazy collection fetches, commonly in one-to-many relationships where accessing a nested collection can reveal an N+1 problem.

You can combine annotations on the same test when you want both a high-level query budget and precise Hibernate statistic expectations:

```java
@Test
@ExpectMaxQueries(7)
@ExpectQueryExecutionCount(2)
@ExpectCollectionFetchCount(5)
void listCompaniesAndFetchEmployees() {
    companyService.list(0, 5);
}
```

## Devlog & Video Series
[![Watch the video](https://img.youtube.com/vi/nF8DMHj-fmY/maxresdefault.jpg)](https://www.youtube.com/watch?v=nF8DMHj-fmY)


This project is documented as a devlog, showing how an idea can turn into a project. Follow the development journey on YouTube:

- **YouTube Channel:** [bitaligners](https://www.youtube.com/@bitaligners)
- **Devlog Playlist:** [From Idea to Project: N+1 Query Problem Detector](https://www.youtube.com/watch?v=nF8DMHj-fmY&list=PLIoJ8eJvtC1apnL1jv65VPvr4Q16d04Gc&pp=gAQB)

## License

This project is licensed under the Apache License 2.0.

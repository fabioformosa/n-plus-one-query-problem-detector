# N+1 Query Problem Detector

A Spring Java testing library for detecting the N+1 query problem in Hibernate-based applications.

## Context: What is the N+1 Query Problem?

The N+1 query problem is a common performance issue in ORM frameworks like Hibernate. It occurs when an application executes one query to fetch a list of entities (the "1"), and then, for each entity, executes an additional query (the "N") to fetch related data. This can lead to a large number of unnecessary database queries, severely impacting performance.

## What is N+1 Query Problem Detector?

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
   - **Currently:** The library is not yet on Maven Central. Clone this repository and run `./gradlew publishToMavenLocal` to install it locally, then use the dependency as above.

2. **Enable Hibernate statistics**
   - In your `application.properties` (test profile):
     ```properties
     spring.jpa.properties.hibernate.generate_statistics=true
     ```

3. **Write your test**
   - Inject `NPlusOneQueryProblemDetector` into your test class.
   - Use `startMonitoring()` and `stopMonitoring()` to bracket the code you want to monitor.
   - Use the assertion utilities to check for N+1 problems.

   Example:
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

## Devlog & Video Series

This project is documented as a devlog, showing how an idea can turn into a project. Follow the development journey on YouTube:

- **YouTube Channel:** [bitaligners](https://www.youtube.com/@bitaligners)
- **Devlog Playlist:** [From Idea to Project: N+1 Query Problem Detector](https://www.youtube.com/watch?v=nF8DMHj-fmY&list=PLIoJ8eJvtC1apnL1jv65VPvr4Q16d04Gc&pp=gAQB)

## License

This project is licensed under the Apache License 2.0.

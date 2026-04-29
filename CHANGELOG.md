# Changelog

## Unreleased
- Coming Soon

## 0.2.2
- `FIX` the build phase including javaSource and javaDoc

## 0.2.1
- `FIX` improved the transitive dependency management to avoid conflicts with other libraries that depend on Hibernate, such as Spring Data JPA and tested with Spring Boot 4.0.6 also

## 0.2.0
- `NEW FEATURE` The hibernate statistics are now enabled automatically when monitoring starts, so you do not need to set `spring.jpa.properties.hibernate.generate_statistics=true` in your test configuration.

## 0.1.0
- `NEW FEATURE` Added a JUnit 5 extension to start and stop monitoring automatically around test methods.
- `NEW FEATURE` Added assertion annotations for max queries, query execution count, entity fetch count, and collection fetch count.
- `DOCS` Reworked the README with a Get Started section and a deeper assertions guide.

## 0.0.2 - 0.0.10
-  `FIX` fixed the CICD pipeline to properly create releases on GitHub.

## 0.0.1
-  `NEW FEATURE` Add a changelog file and released the first version 0.0.1 to test the
    CI/CD pipeline based on a GitHub Actions workflow.

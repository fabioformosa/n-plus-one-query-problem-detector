package it.fabioformosa.nplusonequeryproblemdetector.sampleProductTests;

import it.fabioformosa.nplusonequeryproblemdetector.engine.NPlusOneQueryProblemAssertions;
import it.fabioformosa.nplusonequeryproblemdetector.engine.NPlusOneQueryProblemDetector;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.CompanyDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.PaginatedListDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.services.CompanyService;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AbstractIntegrationTestSuite;
import it.fabioformosa.nplusonequeryproblemdetector.utilities.AsciiLogUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CompanyServiceWithDetectorIntegrationTest extends AbstractIntegrationTestSuite {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private NPlusOneQueryProblemDetector detector;

    @Test
    void givenCompaniesWithAssociationEmployees_whenTheFetchTypeIsLazy_thenTheDetectorCollectsNPlusOneStats() {
        detector.startMonitoring();

        int pageSize = 5;
        PaginatedListDto<CompanyDto> companyDtoList = companyService.list(0, pageSize);
        AsciiLogUtils.displayEntitiesViaLogs(
                companyDtoList.getItems(),
                new String[] { "ID", "Name", "Employees" },
                c -> new Object[] { c.getId(), c.getName(), c.getEmployees().size() }
        );

        detector.stopMonitoring();

        Assertions.assertThat(companyDtoList.getTotalItems()).isEqualTo(10);
        Assertions.assertThat(companyDtoList.getItems()).hasSize(pageSize);
        Assertions.assertThat(companyDtoList.getTotalPages()).isEqualTo(2);

        NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(7); // put 7 just to make the test green, but we're in front of a n+1 query problem
        NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).queryExecutionCountIsEqualTo(2);
        NPlusOneQueryProblemAssertions.assertThat(detector.getMonitoredStats()).collectionFetchCountIsEqualTo(pageSize);
    }

    @Test
    void givenCompaniesWithAssociationEmployees_whenTheQueryFetchesExplicitlyViaJQL_thenTheNPlus1QueryProblemIsNotPresent() {
        detector.startMonitoring();

        int pageSize = 5;
        PaginatedListDto<CompanyDto> companyDtoList = companyService.listWithFetchViaJQL(0, pageSize);
        AsciiLogUtils.displayEntitiesViaLogs(
                companyDtoList.getItems(),
                new String[] { "ID", "Name", "Employees" },
                c -> new Object[] { c.getId(), c.getName(), c.getEmployees().size() }
        );

        detector.stopMonitoring();

        Assertions.assertThat(companyDtoList.getTotalItems()).isEqualTo(10);
        Assertions.assertThat(companyDtoList.getItems()).hasSize(pageSize);
        Assertions.assertThat(companyDtoList.getTotalPages()).isEqualTo(2);

        NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(2);
    }

    @Test
    void givenCompaniesWithAssociationEmployees_whenTheQueryFetchesExplicitlyViaSpecification_thenTheNPlus1QueryProblemIsNotPresent() {
        detector.startMonitoring();

        int pageSize = 5;
        PaginatedListDto<CompanyDto> companyDtoList = companyService.listWithFetchViaSpecification(0, pageSize);
        AsciiLogUtils.displayEntitiesViaLogs(
                companyDtoList.getItems(),
                new String[] { "ID", "Name", "Employees" },
                c -> new Object[] { c.getId(), c.getName(), c.getEmployees().size() }
        );

        detector.stopMonitoring();

        Assertions.assertThat(companyDtoList.getTotalItems()).isEqualTo(10);
        Assertions.assertThat(companyDtoList.getItems()).hasSize(pageSize);
        Assertions.assertThat(companyDtoList.getTotalPages()).isEqualTo(2);

        NPlusOneQueryProblemAssertions.assertThat(detector).hasCountedMaxQueries(2);
    }
}

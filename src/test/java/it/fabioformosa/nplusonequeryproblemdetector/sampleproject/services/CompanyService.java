package it.fabioformosa.nplusonequeryproblemdetector.sampleproject.services;

import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.converters.Converter;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.CompanyDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.PaginatedListDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.entities.Company;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.repos.CompanyRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;


@Transactional
@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {

    private final CompanyRepository companyRepository;

    private static void printCompanies(PaginatedListDto<CompanyDto> paginatedListDto) {
        log.info("######### COMPANIES ({} of {}) ############", paginatedListDto.getItems().size(), paginatedListDto.getTotalItems());
        paginatedListDto.getItems().stream().forEach(companyDto -> {
            log.info("{} || {} || #empl. {}", companyDto.getId(), companyDto.getName(), companyDto.getEmployees().size());
        });
        log.info("######### /COMPANIES ############");
    }

    public PaginatedListDto<CompanyDto> list(int pageNum, int pageSize){
        Page<Company> companyPage = companyRepository.findAll(PageRequest.of(pageNum, pageSize, Sort.by("id")));
        return Converter.fromPageToPaginatedListDto(companyPage, Converter::fromCompanyToCompanyDto);
    }

    public PaginatedListDto<CompanyDto> listWithFetchViaJQL(int pageNum, int pageSize){
        Page<Company> companyPage = companyRepository.findPaginatedWithEmployees(PageRequest.of(pageNum, pageSize, Sort.by("id")));
        PaginatedListDto<CompanyDto> paginatedListDto = Converter.fromPageToPaginatedListDto(companyPage, Converter::fromCompanyToCompanyDto);
        printCompanies(paginatedListDto);
        return paginatedListDto;
    }

    public PaginatedListDto<CompanyDto> listWithFetchViaSpecification(int pageNum, int pageSize){
        Page<Company> companyPage = companyRepository.findAll(fetchEmployeesSpecification(), PageRequest.of(pageNum, pageSize, Sort.by("id")));
        PaginatedListDto<CompanyDto> paginatedListDto = Converter.fromPageToPaginatedListDto(companyPage, Converter::fromCompanyToCompanyDto);
        printCompanies(paginatedListDto);
        return paginatedListDto;
    }

    private static Specification<Company> fetchEmployeesSpecification() {
        return (root, q, cb) -> {
            if (Long.class != q.getResultType() && long.class != q.getResultType()) {
                root.fetch("employees", JoinType.LEFT);
            }
            return cb.equal(cb.literal(1), 1);
        };
    }

}

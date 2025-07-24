package it.fabioformosa.nplusonequeryproblemdetector.sampleproject.services;

import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.converters.Converter;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.EmployeeDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.PaginatedListDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.entities.Employee;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.repos.EmployeeRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Transactional
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;


    public static Specification<Employee> fetchCompanySpecification() {
        return (root, q, cb) -> {
            if (Long.class != q.getResultType() && long.class != q.getResultType()) {
                root.fetch("company", JoinType.LEFT);
            }
            return cb.equal(cb.literal(1), 1);
        };
    }

    public PaginatedListDto<EmployeeDto> list(int pageNum, int pageSize) {
        Page<Employee> employeePage = employeeRepository.findAll(PageRequest.of(pageNum, pageSize, Sort.by("id")));
        PaginatedListDto<EmployeeDto> paginatedListDto = Converter.fromPageToPaginatedListDto(employeePage, Converter::fromEmployeeToEmployeeDto);
        return paginatedListDto;
    }

    public PaginatedListDto<EmployeeDto> listWithSpecification(int pageNum, int pageSize){
        Page<Employee> employeePage = employeeRepository.findAll(fetchCompanySpecification(), PageRequest.of(pageNum, pageSize, Sort.by("id")));
        PaginatedListDto<EmployeeDto> paginatedListDto = Converter.fromPageToPaginatedListDto(employeePage, Converter::fromEmployeeToEmployeeDto);
        return paginatedListDto;
    }

    public PaginatedListDto<EmployeeDto> listWithCompany(int pageNum, int pageSize){
        Page<Employee> employeePage = employeeRepository.findPaginatedWithCompany(PageRequest.of(pageNum, pageSize, Sort.by("id")));
        PaginatedListDto<EmployeeDto> paginatedListDto = Converter.fromPageToPaginatedListDto(employeePage, Converter::fromEmployeeToEmployeeDto);
        return paginatedListDto;
    }

}

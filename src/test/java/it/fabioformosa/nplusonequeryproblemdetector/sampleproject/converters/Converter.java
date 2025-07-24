package it.fabioformosa.nplusonequeryproblemdetector.sampleproject.converters;


import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.CompanyDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.EmployeeDto;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.entities.Company;
import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.entities.Employee;

public class Converter extends AbstractBaseConverter {

    public static CompanyDto fromCompanyToCompanyDto(Company company){
        return CompanyDto.builder()
                .id(company.getId())
                .name(company.getName())
                .employees(company.getEmployees().stream().map(Converter::fromEmployeeToEmployeeDto).toList())
                .build();
    }

    public static EmployeeDto fromEmployeeToEmployeeDto(Employee employee) {
        return EmployeeDto.builder()
                .id(employee.getId())
                .lastname(employee.getLastname())
                .firstname(employee.getFirstname())
                .companyName(employee.getCompany().getName())
                .build();
    }

}

package it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmployeeDto {

    private Long id;

    private String firstname;
    private String lastname;

    private String companyName;
}

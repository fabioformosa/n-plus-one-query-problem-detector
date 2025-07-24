package it.fabioformosa.nplusonequeryproblemdetector.sampleproject.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "COMPANIES")
@Data
public class Company {

    @Id
    private Long id;

    private String name;

    //Collections are lazy fetched by default
    @OneToMany(mappedBy="company")
    private List<Employee> employees;
}

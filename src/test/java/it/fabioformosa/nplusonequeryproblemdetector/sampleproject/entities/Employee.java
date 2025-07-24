package it.fabioformosa.nplusonequeryproblemdetector.sampleproject.entities;

import jakarta.persistence.*;
import lombok.Data;


@Entity
@Table(name = "EMPLOYEES")
@Data
public class Employee {

    @Id
    private Long id;

    private String firstname;
    private String lastname;

    @ManyToOne
    @JoinColumn(name="fk_company", nullable = false)
    private Company company;
}

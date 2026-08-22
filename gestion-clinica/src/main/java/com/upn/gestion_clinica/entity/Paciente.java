package com.upn.gestion_clinica.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "paciente")
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "documento_identidad", nullable = false, unique = true, length = 9)
    private String documentoIdentidad;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(nullable = false, length = 15)
    private String telefono;

    @Column(nullable = false, length = 45)
    private String correo;

    @Column(nullable = false, columnDefinition = "SMALLINT DEFAULT 1")
    private Integer estado;
}

package com.upn.gestion_clinica.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "medico")
public class Medico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    @Column(name = "numero_colegiatura", nullable = false, unique = true, length = 50)
    private String numeroColegiatura;
}

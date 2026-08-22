package com.upn.gestion_clinica.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "procedimiento_medico")
public class ProcedimientoMedico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atencion_medica_id", nullable = false, unique = true)
    private AtencionMedica atencionMedica;

    @Column(name = "descripcion_procedimiento", nullable = false, columnDefinition = "TEXT")
    private String descripcionProcedimiento;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String resultado;

}

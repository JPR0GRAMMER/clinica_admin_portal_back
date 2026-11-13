package com.upn.gestion_clinica.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

import java.time.OffsetDateTime;

@Entity
@Table(name = "atencion_medica")
@Getter
@Setter
public class AtencionMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_medica_id", nullable = false, unique = true)
    private CitaMedica citaMedica;

    @Column(name = "motivo_consulta", nullable = false, columnDefinition = "TEXT")
    private String motivoConsulta;

    @Column(nullable = false, length = 255)
    private String diagnostico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_cie10", nullable = false)
    private Cie10 cie10;

    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "fecha_atencion", nullable = false, updatable = false)
    private OffsetDateTime fechaAtencion;

    @OneToMany(mappedBy = "atencionMedica", fetch = FetchType.LAZY)
    private java.util.List<RecetaMedica> recetas;

    @OneToMany(mappedBy = "atencionMedica", fetch = FetchType.LAZY)
    private java.util.List<ProcedimientoMedico> procedimientos;
}

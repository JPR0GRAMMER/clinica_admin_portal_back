package com.upn.gestion_clinica.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "detalle_receta")
public class DetalleReceta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_medica_id", nullable = false)
    private RecetaMedica recetaMedica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @Column(nullable = false, length = 45)
    private String dosis;

    @Column(nullable = false, length = 45)
    private String frecuencia;

    @Column(name = "duracion_tratamiento", nullable = false, length = 45)
    private String duracionTratamiento;

    @Column(name = "cantidad_prescrita", nullable = false)
    private Integer cantidadPrescrita;

}

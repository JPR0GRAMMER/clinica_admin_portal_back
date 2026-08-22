package com.upn.gestion_clinica.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "medicamento")
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "nombre_comercial", nullable = false, length = 100)
    private String nombreComercial;

    @Column(name = "principio_activo", nullable = false, length = 100)
    private String principioActivo;

    @Column(nullable = false, length = 45)
    private String presentacion;

    @Column(nullable = false, length = 20)
    private String concentracion;

    @Column(name = "unidad_medida", nullable = false, length = 20)
    private String unidadMedida;

    @Column(nullable = false)
    private Integer stock = 0;

}

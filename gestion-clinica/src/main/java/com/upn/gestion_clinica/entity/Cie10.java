package com.upn.gestion_clinica.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cie10")
@Getter
@Setter
public class Cie10 {

    @Id
    @Column(length = 7, nullable = false)
    private String codigo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;
}

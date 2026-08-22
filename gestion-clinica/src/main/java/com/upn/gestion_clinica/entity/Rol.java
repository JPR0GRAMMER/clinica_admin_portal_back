package com.upn.gestion_clinica.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "rol")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nombre;

    private String descripcion;

    @ManyToMany(fetch = jakarta.persistence.FetchType.EAGER)
    @jakarta.persistence.JoinTable(
            name = "rol_vista",
            joinColumns = @jakarta.persistence.JoinColumn(name = "rol_id"),
            inverseJoinColumns = @jakarta.persistence.JoinColumn(name = "vista_id")
    )
    private Set<Vista> vistas;
}

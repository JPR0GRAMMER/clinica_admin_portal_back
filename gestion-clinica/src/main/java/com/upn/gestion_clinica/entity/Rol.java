package com.upn.gestion_clinica.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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

    @Column(nullable = false, unique = true, length = 45)
    private String nombre;

    @Column(nullable = false, length = 45)
    private String descripcion = "Rol del sistema";

    @Column(nullable = false, columnDefinition = "SMALLINT DEFAULT 1")
    private Integer estado = 1;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "rol_vista",
            joinColumns = @JoinColumn(name = "rol_id"),
            inverseJoinColumns = @JoinColumn(name = "vista_id")
    )
    @OrderBy("nombre ASC")
    private Set<Vista> vistas;

    @PrePersist
    @PreUpdate
    public void asegurarValoresPorDefecto() {
        if (this.descripcion == null || this.descripcion.isBlank()) {
            this.descripcion = "Rol " + (this.nombre != null ? this.nombre : "del sistema");
        }
        if (this.estado == null) {
            this.estado = 1;
        }
    }
}
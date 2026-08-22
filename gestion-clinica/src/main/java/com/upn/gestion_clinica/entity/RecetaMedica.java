package com.upn.gestion_clinica.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "receta_medica")
public class RecetaMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atencion_medica_id", nullable = false)
    private AtencionMedica atencionMedica;

    @Column(name = "indicaciones_generales", nullable = false, columnDefinition = "TEXT")
    private String indicacionesGenerales;

    @Column(name = "fecha_emision", nullable = false, updatable = false)
    private LocalDateTime fechaEmision;

    @OneToMany(mappedBy = "recetaMedica", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleReceta> detalles = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        fechaEmision = LocalDateTime.now();
    }


    public void addDetalle(DetalleReceta detalle) {
        detalles.add(detalle);
        detalle.setRecetaMedica(this);
    }

    public void removeDetalle(DetalleReceta detalle) {
        detalles.remove(detalle);
        detalle.setRecetaMedica(null);
    }
}

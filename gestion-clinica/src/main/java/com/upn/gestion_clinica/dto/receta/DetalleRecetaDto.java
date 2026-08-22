package com.upn.gestion_clinica.dto.receta;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DetalleRecetaDto {
    private Long medicamentoId;
    private String medicamentoNombre;
    private String medicamentoPrincipioActivo;
    private String dosis;
    private String frecuencia;
    private String duracionTratamiento;
    private Integer cantidadPrescrita;
}

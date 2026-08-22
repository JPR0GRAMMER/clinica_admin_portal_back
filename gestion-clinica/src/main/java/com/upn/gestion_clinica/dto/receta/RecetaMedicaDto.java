package com.upn.gestion_clinica.dto.receta;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecetaMedicaDto {
    private String indicacionesGenerales;
    private List<DetalleRecetaDto> detalles;
}

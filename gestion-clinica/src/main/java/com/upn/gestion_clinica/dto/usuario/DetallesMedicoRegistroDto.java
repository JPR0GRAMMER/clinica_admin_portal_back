package com.upn.gestion_clinica.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DetallesMedicoRegistroDto {
    @NotBlank(message = "El número de colegiatura es obligatorio para médicos.")
    private String numeroColegiatura;

    @NotNull(message = "La especialidad es obligatoria para médicos.")
    private Integer especialidadId;
}

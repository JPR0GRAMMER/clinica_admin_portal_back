package com.upn.gestion_clinica.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DetallesFarmaceuticoDto {

    @NotBlank(message = "El número de colegiatura (CQFP) es obligatorio para el farmacéutico.")
    private String numeroColegiatura;

}

package com.upn.gestion_clinica.dto.usuario;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MedicoResponseDto {
    private Integer id;
    private String nombre;
    private String apellido;
    private String especialidad;
    private String numeroColegiatura;
}

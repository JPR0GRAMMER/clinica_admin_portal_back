package com.upn.gestion_clinica.dto.usuario;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonInclude;

@Getter
@Setter
@JsonPropertyOrder({"id", "nombre", "apellido", "correo", "rolNombre", "estado", "detallesMedico", "detallesFarmaceutico"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsuarioResponseDto {

    private Integer id;
    private String nombre;
    private String apellido;
    private String correo;
    private String rolNombre;
    private Integer estado;
    private DetallesMedicoDto detallesMedico;
    private DetallesFarmaceuticoDto detallesFarmaceutico;
}

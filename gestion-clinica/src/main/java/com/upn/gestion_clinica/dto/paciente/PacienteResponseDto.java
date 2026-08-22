package com.upn.gestion_clinica.dto.paciente;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDate;

@Getter
@Setter
@JsonPropertyOrder({"id", "documentoIdentidad", "nombre", "apellido", "fechaNacimiento", "telefono", "correo", "estado"})
public class PacienteResponseDto {

    private Integer id;
    private String documentoIdentidad;
    private String nombre;
    private String apellido;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String correo;
    private Integer estado;
}

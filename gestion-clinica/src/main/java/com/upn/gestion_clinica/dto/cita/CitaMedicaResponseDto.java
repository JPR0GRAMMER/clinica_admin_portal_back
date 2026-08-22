package com.upn.gestion_clinica.dto.cita;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class CitaMedicaResponseDto {
    private Integer id;
    
    private Integer pacienteId;
    private String pacienteNombreCompleto;
    private String pacienteDni;
    
    private Integer medicoId;
    private String medicoNombreCompleto;
    private String medicoEspecialidad;
    
    private LocalDate fechaCita;
    private LocalTime horaCita;
    private String estadoCita;
}

package com.upn.gestion_clinica.dto.horario_medico;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.LocalTime;

@Getter
@Setter
@JsonPropertyOrder({"id", "medicoId", "medicoNombreCompleto", "diaSemana", "horaInicio", "horaFin"})
public class HorarioMedicoResponseDto {
    private Integer id;
    private Integer medicoId;
    private String medicoNombreCompleto;
    private Integer diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
}

package com.upn.gestion_clinica.dto.horario_medico;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class HorarioMedicoRequestDto {

    @NotNull(message = "El ID del médico es obligatorio.")
    private Integer medicoId;

    @NotNull(message = "El día de la semana es obligatorio.")
    @Min(value = 1, message = "El día de la semana debe ser al menos 1 (Lunes).")
    @Max(value = 7, message = "El día de la semana no puede ser mayor a 7 (Domingo).")
    private Integer diaSemana;

    @NotNull(message = "La hora de inicio es obligatoria.")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria.")
    private LocalTime horaFin;
}

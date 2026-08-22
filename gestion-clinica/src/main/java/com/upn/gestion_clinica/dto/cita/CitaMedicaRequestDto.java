package com.upn.gestion_clinica.dto.cita;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class CitaMedicaRequestDto {

    @NotNull(message = "El ID del paciente es obligatorio")
    private Integer pacienteId;

    @NotNull(message = "El ID del médico es obligatorio")
    private Integer medicoId;

    @NotNull(message = "La fecha de la cita es obligatoria")
    private LocalDate fechaCita;

    @NotNull(message = "La hora de la cita es obligatoria")
    private LocalTime horaCita;
}

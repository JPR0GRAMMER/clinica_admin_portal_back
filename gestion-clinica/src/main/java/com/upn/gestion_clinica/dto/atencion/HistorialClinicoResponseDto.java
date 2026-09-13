package com.upn.gestion_clinica.dto.atencion;

import com.upn.gestion_clinica.dto.paciente.PacienteResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class HistorialClinicoResponseDto {
    private PacienteResponseDto paciente;
    private List<AtencionMedicaResponseDto> atenciones;
}

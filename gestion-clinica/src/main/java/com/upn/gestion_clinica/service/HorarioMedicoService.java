package com.upn.gestion_clinica.service;

import com.upn.gestion_clinica.dto.horario_medico.HorarioMedicoRequestDto;
import com.upn.gestion_clinica.dto.horario_medico.HorarioMedicoResponseDto;
import java.util.List;

public interface HorarioMedicoService {
    HorarioMedicoResponseDto crear(HorarioMedicoRequestDto requestDto);
    HorarioMedicoResponseDto obtenerPorId(Integer id);
    List<HorarioMedicoResponseDto> listarTodos();
    List<HorarioMedicoResponseDto> listarPorMedico(Integer medicoId);
    List<HorarioMedicoResponseDto> listarPorEspecialidad(Integer especialidadId);
    HorarioMedicoResponseDto actualizar(Integer id, HorarioMedicoRequestDto requestDto);
    void eliminar(Integer id);
}

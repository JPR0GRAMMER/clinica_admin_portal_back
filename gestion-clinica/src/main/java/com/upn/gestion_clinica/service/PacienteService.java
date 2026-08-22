package com.upn.gestion_clinica.service;

import java.util.List;

import com.upn.gestion_clinica.dto.paciente.PacienteRequestDto;
import com.upn.gestion_clinica.dto.paciente.PacienteResponseDto;

public interface PacienteService {
    PacienteResponseDto crear(PacienteRequestDto requestDto);
    PacienteResponseDto obtenerPorId(Integer id);
    List<PacienteResponseDto> listarTodos();
    PacienteResponseDto actualizar(Integer id, PacienteRequestDto requestDto);
    void cambiarEstado(Integer id, Integer estado);
}

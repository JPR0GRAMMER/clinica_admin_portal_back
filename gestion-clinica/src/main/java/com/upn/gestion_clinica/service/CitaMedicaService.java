package com.upn.gestion_clinica.service;

import com.upn.gestion_clinica.dto.cita.CitaMedicaRequestDto;
import com.upn.gestion_clinica.dto.cita.CitaMedicaResponseDto;
import com.upn.gestion_clinica.entity.EstadoCitaEnum;

import java.util.List;

public interface CitaMedicaService {
    CitaMedicaResponseDto agendarCita(CitaMedicaRequestDto requestDto);
    List<CitaMedicaResponseDto> listarCitas(String correoMedico, boolean esMedico);
    CitaMedicaResponseDto obtenerCitaPorId(Integer id, String correoMedico, boolean esMedico);
    CitaMedicaResponseDto reprogramarCita(Integer id, CitaMedicaRequestDto requestDto);
    void cambiarEstadoCita(Integer id, EstadoCitaEnum nuevoEstado);
}

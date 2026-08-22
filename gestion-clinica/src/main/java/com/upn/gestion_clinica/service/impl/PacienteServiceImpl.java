package com.upn.gestion_clinica.service.impl;

import com.upn.gestion_clinica.dto.paciente.PacienteRequestDto;
import com.upn.gestion_clinica.dto.paciente.PacienteResponseDto;
import com.upn.gestion_clinica.entity.Paciente;
import com.upn.gestion_clinica.repository.PacienteRepository;
import com.upn.gestion_clinica.service.PacienteService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PacienteServiceImpl implements PacienteService {

    private final PacienteRepository pacienteRepository;

    public PacienteServiceImpl(PacienteRepository pacienteRepository) {
        this.pacienteRepository = pacienteRepository;
    }

    @Override
    @Transactional
    public PacienteResponseDto crear(PacienteRequestDto requestDto) {
        if (pacienteRepository.existsByDocumentoIdentidad(requestDto.getDocumentoIdentidad())) {
            throw new IllegalArgumentException("Ya existe un paciente con el documento de identidad: " + requestDto.getDocumentoIdentidad());
        }

        Paciente paciente = new Paciente();
        mapearAPaciente(requestDto, paciente);
        paciente.setEstado(1);

        Paciente guardado = pacienteRepository.save(paciente);
        return mapearAResponseDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public PacienteResponseDto obtenerPorId(Integer id) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Paciente no encontrado con id: " + id));
        return mapearAResponseDto(paciente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PacienteResponseDto> listarTodos() {
        return pacienteRepository.findAll().stream()
                .map(this::mapearAResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PacienteResponseDto actualizar(Integer id, PacienteRequestDto requestDto) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Paciente no encontrado con id: " + id));


        if (!paciente.getDocumentoIdentidad().equals(requestDto.getDocumentoIdentidad()) &&
            pacienteRepository.existsByDocumentoIdentidad(requestDto.getDocumentoIdentidad())) {
            throw new IllegalArgumentException("Ya existe otro paciente con el documento de identidad: " + requestDto.getDocumentoIdentidad());
        }

        mapearAPaciente(requestDto, paciente);
        Paciente actualizado = pacienteRepository.save(paciente);
        return mapearAResponseDto(actualizado);
    }

    @Override
    @Transactional
    public void cambiarEstado(Integer id, Integer estado) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Paciente no encontrado con id: " + id));
        paciente.setEstado(estado);
        pacienteRepository.save(paciente);
    }

    private void mapearAPaciente(PacienteRequestDto requestDto, Paciente paciente) {
        paciente.setDocumentoIdentidad(requestDto.getDocumentoIdentidad());
        paciente.setNombre(requestDto.getNombre());
        paciente.setApellido(requestDto.getApellido());
        paciente.setFechaNacimiento(requestDto.getFechaNacimiento());
        paciente.setTelefono(requestDto.getTelefono());
        paciente.setCorreo(requestDto.getCorreo());
    }

    private PacienteResponseDto mapearAResponseDto(Paciente paciente) {
        PacienteResponseDto responseDto = new PacienteResponseDto();
        responseDto.setId(paciente.getId());
        responseDto.setDocumentoIdentidad(paciente.getDocumentoIdentidad());
        responseDto.setNombre(paciente.getNombre());
        responseDto.setApellido(paciente.getApellido());
        responseDto.setFechaNacimiento(paciente.getFechaNacimiento());
        responseDto.setTelefono(paciente.getTelefono());
        responseDto.setCorreo(paciente.getCorreo());
        responseDto.setEstado(paciente.getEstado());
        return responseDto;
    }
}

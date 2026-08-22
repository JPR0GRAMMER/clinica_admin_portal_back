package com.upn.gestion_clinica.controller;

import com.upn.gestion_clinica.dto.usuario.MedicoResponseDto;
import com.upn.gestion_clinica.repository.MedicoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/medicos")
public class MedicoController {

    private final MedicoRepository medicoRepository;

    public MedicoController(MedicoRepository medicoRepository) {
        this.medicoRepository = medicoRepository;
    }

    @GetMapping
    public ResponseEntity<List<MedicoResponseDto>> listarMedicos() {
        List<MedicoResponseDto> medicos = medicoRepository.findAll().stream().map(medico -> {
            MedicoResponseDto dto = new MedicoResponseDto();
            dto.setId(medico.getId());
            dto.setNombre(medico.getUsuario().getNombre());
            dto.setApellido(medico.getUsuario().getApellido());
            dto.setEspecialidad(medico.getEspecialidad().getNombre());
            dto.setNumeroColegiatura(medico.getNumeroColegiatura());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(medicos);
    }
}

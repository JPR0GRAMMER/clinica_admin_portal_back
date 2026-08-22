package com.upn.gestion_clinica.controller;

import com.upn.gestion_clinica.dto.paciente.PacienteRequestDto;
import com.upn.gestion_clinica.dto.paciente.PacienteResponseDto;
import com.upn.gestion_clinica.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;

    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @PostMapping
    public ResponseEntity<PacienteResponseDto> crearPaciente(@Valid @RequestBody PacienteRequestDto requestDto) {
        PacienteResponseDto response = pacienteService.crear(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PacienteResponseDto> obtenerPaciente(@PathVariable Integer id) {
        PacienteResponseDto response = pacienteService.obtenerPorId(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<PacienteResponseDto>> listarPacientes() {
        List<PacienteResponseDto> response = pacienteService.listarTodos();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PacienteResponseDto> actualizarPaciente(@PathVariable Integer id, @Valid @RequestBody PacienteRequestDto requestDto) {
        PacienteResponseDto response = pacienteService.actualizar(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivarPaciente(@PathVariable Integer id) {
        pacienteService.cambiarEstado(id, 0);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activar")
    public ResponseEntity<Void> activarPaciente(@PathVariable Integer id) {
        pacienteService.cambiarEstado(id, 1);
        return ResponseEntity.noContent().build();
    }
}

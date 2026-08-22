package com.upn.gestion_clinica.controller;

import com.upn.gestion_clinica.dto.horario_medico.HorarioMedicoRequestDto;
import com.upn.gestion_clinica.dto.horario_medico.HorarioMedicoResponseDto;
import com.upn.gestion_clinica.service.HorarioMedicoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/horarios-medicos")
public class HorarioMedicoController {

    private final HorarioMedicoService horarioMedicoService;

    public HorarioMedicoController(HorarioMedicoService horarioMedicoService) {
        this.horarioMedicoService = horarioMedicoService;
    }

    @PostMapping
    public ResponseEntity<HorarioMedicoResponseDto> crear(@Valid @RequestBody HorarioMedicoRequestDto requestDto) {
        HorarioMedicoResponseDto response = horarioMedicoService.crear(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HorarioMedicoResponseDto> obtenerPorId(@PathVariable Integer id) {
        HorarioMedicoResponseDto response = horarioMedicoService.obtenerPorId(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<HorarioMedicoResponseDto>> listarTodos() {
        List<HorarioMedicoResponseDto> response = horarioMedicoService.listarTodos();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/medico/{medicoId}")
    public ResponseEntity<List<HorarioMedicoResponseDto>> listarPorMedico(@PathVariable Integer medicoId) {
        List<HorarioMedicoResponseDto> response = horarioMedicoService.listarPorMedico(medicoId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/especialidad/{especialidadId}")
    public ResponseEntity<List<HorarioMedicoResponseDto>> listarPorEspecialidad(@PathVariable Integer especialidadId) {
        List<HorarioMedicoResponseDto> response = horarioMedicoService.listarPorEspecialidad(especialidadId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HorarioMedicoResponseDto> actualizar(@PathVariable Integer id,
            @Valid @RequestBody HorarioMedicoRequestDto requestDto) {
        HorarioMedicoResponseDto response = horarioMedicoService.actualizar(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        horarioMedicoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

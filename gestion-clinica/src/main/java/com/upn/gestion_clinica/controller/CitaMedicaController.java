package com.upn.gestion_clinica.controller;

import com.upn.gestion_clinica.dto.cita.CitaMedicaRequestDto;
import com.upn.gestion_clinica.dto.cita.CitaMedicaResponseDto;
import com.upn.gestion_clinica.entity.EstadoCitaEnum;
import com.upn.gestion_clinica.service.CitaMedicaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@RestController
@RequestMapping("/api/citas")
public class CitaMedicaController {

    private final CitaMedicaService citaMedicaService;

    public CitaMedicaController(CitaMedicaService citaMedicaService) {
        this.citaMedicaService = citaMedicaService;
    }

    @PostMapping
    public ResponseEntity<CitaMedicaResponseDto> agendarCita(@Valid @RequestBody CitaMedicaRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citaMedicaService.agendarCita(dto));
    }

    private String getCorreoLogueado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    private boolean isMedicoLogueado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().toUpperCase().contains("MEDICO"));
    }

    @GetMapping
    public ResponseEntity<List<CitaMedicaResponseDto>> listarCitas() {
        return ResponseEntity.ok(citaMedicaService.listarCitas(getCorreoLogueado(), isMedicoLogueado()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CitaMedicaResponseDto> obtenerCita(@PathVariable Integer id) {
        return ResponseEntity.ok(citaMedicaService.obtenerCitaPorId(id, getCorreoLogueado(), isMedicoLogueado()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CitaMedicaResponseDto> reprogramarCita(@PathVariable Integer id, @Valid @RequestBody CitaMedicaRequestDto dto) {
        return ResponseEntity.ok(citaMedicaService.reprogramarCita(id, dto));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Void> cambiarEstado(@PathVariable Integer id, @RequestParam String estado) {

        EstadoCitaEnum nuevoEstado = EstadoCitaEnum.valueOf(estado.replace(" ", "_"));
        citaMedicaService.cambiarEstadoCita(id, nuevoEstado);
        return ResponseEntity.noContent().build();
    }
}

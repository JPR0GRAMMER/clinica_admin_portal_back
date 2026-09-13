package com.upn.gestion_clinica.controller;

import com.upn.gestion_clinica.dto.atencion.AtencionMedicaRegistroDto;
import com.upn.gestion_clinica.dto.atencion.AtencionMedicaResponseDto;
import com.upn.gestion_clinica.dto.atencion.HistorialClinicoResponseDto;
import com.upn.gestion_clinica.service.AtencionMedicaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/atenciones")
public class AtencionMedicaController {

    private final AtencionMedicaService atencionMedicaService;

    public AtencionMedicaController(AtencionMedicaService atencionMedicaService) {
        this.atencionMedicaService = atencionMedicaService;
    }

    private String getCorreoLogueado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    @PostMapping
    public ResponseEntity<AtencionMedicaResponseDto> registrarAtencion(@Valid @RequestBody AtencionMedicaRegistroDto requestDto) {
        AtencionMedicaResponseDto guardada = atencionMedicaService.registrarAtencion(requestDto, getCorreoLogueado());
        return new ResponseEntity<>(guardada, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AtencionMedicaResponseDto>> listarMisAtenciones() {
        return ResponseEntity.ok(atencionMedicaService.listarMisAtenciones(getCorreoLogueado()));
    }

    @GetMapping("/historial/citas/{citaMedicaId}")
    public ResponseEntity<HistorialClinicoResponseDto> obtenerHistorialPorCita(@PathVariable Integer citaMedicaId) {
        return ResponseEntity.ok(atencionMedicaService.obtenerHistorialPorCita(citaMedicaId, getCorreoLogueado()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AtencionMedicaResponseDto> obtenerAtencion(@PathVariable Integer id) {
        return ResponseEntity.ok(atencionMedicaService.obtenerAtencion(id, getCorreoLogueado()));
    }
}

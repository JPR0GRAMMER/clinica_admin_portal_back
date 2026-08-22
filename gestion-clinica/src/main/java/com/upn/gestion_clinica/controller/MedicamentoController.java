package com.upn.gestion_clinica.controller;

import com.upn.gestion_clinica.entity.Medicamento;
import com.upn.gestion_clinica.service.MedicamentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicamentos")
public class MedicamentoController {

    private final MedicamentoService medicamentoService;

    public MedicamentoController(MedicamentoService medicamentoService) {
        this.medicamentoService = medicamentoService;
    }

    @GetMapping
    public ResponseEntity<List<Medicamento>> listarMedicamentos() {
        return ResponseEntity.ok(medicamentoService.listarTodos());
    }
}

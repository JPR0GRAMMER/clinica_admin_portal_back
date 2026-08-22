package com.upn.gestion_clinica.controller;

import com.upn.gestion_clinica.dto.Cie10ResponseDto;
import com.upn.gestion_clinica.service.Cie10Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cie10")
public class Cie10Controller {

    private final Cie10Service cie10Service;

    public Cie10Controller(Cie10Service cie10Service) {
        this.cie10Service = cie10Service;
    }

    @GetMapping
    public ResponseEntity<List<Cie10ResponseDto>> listarTodos() {
        return ResponseEntity.ok(cie10Service.listarTodos());
    }
}

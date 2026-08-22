package com.upn.gestion_clinica.controller;

import com.upn.gestion_clinica.entity.Especialidad;
import com.upn.gestion_clinica.repository.EspecialidadRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/especialidades")
public class EspecialidadController {

    private final EspecialidadRepository especialidadRepository;

    EspecialidadController(EspecialidadRepository especialidadRepository) {
        this.especialidadRepository = especialidadRepository;
    }

    @GetMapping
    public List<Especialidad> listarEspecialidades() {
        return especialidadRepository.findAll();
    }
}

package com.upn.gestion_clinica.service.impl;

import com.upn.gestion_clinica.dto.Cie10ResponseDto;
import com.upn.gestion_clinica.entity.Cie10;
import com.upn.gestion_clinica.repository.Cie10Repository;
import com.upn.gestion_clinica.service.Cie10Service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class Cie10ServiceImpl implements Cie10Service {

    private final Cie10Repository cie10Repository;

    public Cie10ServiceImpl(Cie10Repository cie10Repository) {
        this.cie10Repository = cie10Repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cie10ResponseDto> listarTodos() {
        return cie10Repository.findAll().stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    private Cie10ResponseDto mapearADto(Cie10 cie10) {
        Cie10ResponseDto dto = new Cie10ResponseDto();
        dto.setCodigo(cie10.getCodigo());
        dto.setDescripcion(cie10.getDescripcion());
        return dto;
    }
}

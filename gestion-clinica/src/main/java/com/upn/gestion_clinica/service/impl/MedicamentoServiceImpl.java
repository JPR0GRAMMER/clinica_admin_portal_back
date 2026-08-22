package com.upn.gestion_clinica.service.impl;

import com.upn.gestion_clinica.entity.Medicamento;
import com.upn.gestion_clinica.repository.MedicamentoRepository;
import com.upn.gestion_clinica.service.MedicamentoService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicamentoServiceImpl implements MedicamentoService {

    private final MedicamentoRepository medicamentoRepository;

    public MedicamentoServiceImpl(MedicamentoRepository medicamentoRepository) {
        this.medicamentoRepository = medicamentoRepository;
    }



    @Override
    public List<Medicamento> listarTodos() {
        return medicamentoRepository.findAll();
    }
}

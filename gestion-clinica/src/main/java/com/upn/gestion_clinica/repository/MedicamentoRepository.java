package com.upn.gestion_clinica.repository;

import com.upn.gestion_clinica.entity.Medicamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {
}

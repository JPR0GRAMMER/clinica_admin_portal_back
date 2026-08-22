package com.upn.gestion_clinica.repository;

import com.upn.gestion_clinica.entity.ProcedimientoMedico;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcedimientoMedicoRepository extends JpaRepository<ProcedimientoMedico, Long> {
}

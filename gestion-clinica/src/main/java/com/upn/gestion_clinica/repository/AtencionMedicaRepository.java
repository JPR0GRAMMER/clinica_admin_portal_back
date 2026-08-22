package com.upn.gestion_clinica.repository;

import com.upn.gestion_clinica.entity.AtencionMedica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AtencionMedicaRepository extends JpaRepository<AtencionMedica, Integer> {

    List<AtencionMedica> findAllByOrderByFechaAtencionDesc();

    Optional<AtencionMedica> findByCitaMedicaId(Integer citaMedicaId);


    List<AtencionMedica> findByCitaMedicaMedicoIdOrderByFechaAtencionDesc(Integer medicoId);
}

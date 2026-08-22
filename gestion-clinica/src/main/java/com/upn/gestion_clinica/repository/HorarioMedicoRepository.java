package com.upn.gestion_clinica.repository;

import com.upn.gestion_clinica.entity.HorarioMedico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioMedicoRepository extends JpaRepository<HorarioMedico, Integer> {
    List<HorarioMedico> findByMedicoId(Integer medicoId);
    List<HorarioMedico> findByMedicoIdAndDiaSemana(Integer medicoId, Integer diaSemana);
    List<HorarioMedico> findByMedicoEspecialidadId(Integer especialidadId);
}

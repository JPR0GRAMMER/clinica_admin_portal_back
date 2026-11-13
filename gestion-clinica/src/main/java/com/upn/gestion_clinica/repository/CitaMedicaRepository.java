package com.upn.gestion_clinica.repository;

import com.upn.gestion_clinica.entity.CitaMedica;
import com.upn.gestion_clinica.entity.EstadoCitaEnum;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CitaMedicaRepository extends JpaRepository<CitaMedica, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CitaMedica c where c.id = :id")
    Optional<CitaMedica> buscarParaActualizar(@Param("id") Integer id);

    boolean existsByMedicoIdAndFechaCitaAndHoraCitaAndEstadoCitaNotAndIdNot(
            Integer medicoId, LocalDate fechaCita, LocalTime horaCita, EstadoCitaEnum estado, Integer idExcluido);

    boolean existsByPacienteIdAndFechaCitaAndHoraCitaAndEstadoCitaNotAndIdNot(
            Integer pacienteId, LocalDate fechaCita, LocalTime horaCita, EstadoCitaEnum estado, Integer idExcluido);

    boolean existsByPacienteIdAndMedicoEspecialidadIdAndFechaCitaAndEstadoCitaNotAndIdNot(
            Integer pacienteId, Integer especialidadId, LocalDate fechaCita, EstadoCitaEnum estado, Integer idExcluido);
    
    List<CitaMedica> findAllByOrderByFechaCitaDescHoraCitaDesc();
    
    List<CitaMedica> findAllByMedicoIdOrderByFechaCitaDescHoraCitaDesc(Integer medicoId);

    List<CitaMedica> findByMedicoIdAndFechaCitaGreaterThanEqualAndEstadoCitaIn(
            Integer medicoId, LocalDate fechaCita, Collection<EstadoCitaEnum> estados);
}

package com.upn.gestion_clinica.repository;

import com.upn.gestion_clinica.entity.CitaMedica;
import com.upn.gestion_clinica.entity.EstadoCitaEnum;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CitaMedicaRepository extends JpaRepository<CitaMedica, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CitaMedica c WHERE c.id = :id")
    Optional<CitaMedica> buscarParaActualizar(@Param("id") Integer id);

    List<CitaMedica> findAllByMedicoIdOrderByFechaCitaDescHoraCitaDesc(Integer medicoId);

    List<CitaMedica> findAllByOrderByFechaCitaDescHoraCitaDesc();

    boolean existsByPacienteIdAndMedicoEspecialidadIdAndFechaCitaAndEstadoCitaNotAndIdNot(
            Integer pacienteId,
            Integer especialidadId,
            LocalDate fechaCita,
            EstadoCitaEnum estadoCita,
            Integer id
    );

    boolean existsByMedicoIdAndFechaCitaAndHoraCitaAndEstadoCitaNotAndIdNot(
            Integer medicoId,
            LocalDate fechaCita,
            LocalTime horaCita,
            EstadoCitaEnum estadoCita,
            Integer id
    );

    boolean existsByPacienteIdAndFechaCitaAndHoraCitaAndEstadoCitaNotAndIdNot(
            Integer pacienteId,
            LocalDate fechaCita,
            LocalTime horaCita,
            EstadoCitaEnum estadoCita,
            Integer id
    );

    List<CitaMedica> findByMedicoIdAndFechaCitaGreaterThanEqualAndEstadoCitaIn(
            Integer medicoId,
            LocalDate fechaCita,
            List<EstadoCitaEnum> estados
    );

    // Métodos adicionales para compatibilidad con suites de pruebas
    boolean existsByMedicoIdAndFechaCitaAndHoraCita(
            Integer medicoId,
            LocalDate fechaCita,
            LocalTime horaCita
    );

    boolean existsByPacienteIdAndFechaCitaAndHoraCita(
            Integer pacienteId,
            LocalDate fechaCita,
            LocalTime horaCita
    );

    boolean existsByMedicoIdAndFechaCitaAndHoraCitaAndEstadoCitaNot(
            Integer medicoId,
            LocalDate fechaCita,
            LocalTime horaCita,
            EstadoCitaEnum estadoCita
    );

    boolean existsByPacienteIdAndFechaCitaAndHoraCitaAndEstadoCitaNot(
            Integer pacienteId,
            LocalDate fechaCita,
            LocalTime horaCita,
            EstadoCitaEnum estadoCita
    );

    boolean existsByPacienteIdAndMedicoEspecialidadIdAndFechaCitaAndEstadoCitaNot(
            Integer pacienteId,
            Integer especialidadId,
            LocalDate fechaCita,
            EstadoCitaEnum estadoCita
    );

    List<CitaMedica> findByMedicoIdAndFechaCitaGreaterThanEqual(
            Integer medicoId,
            LocalDate fechaCita
    );
}
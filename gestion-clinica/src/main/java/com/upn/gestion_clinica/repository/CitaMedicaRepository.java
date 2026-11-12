package com.upn.gestion_clinica.repository;

import com.upn.gestion_clinica.entity.CitaMedica;
import com.upn.gestion_clinica.entity.EstadoCitaEnum;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface CitaMedicaRepository extends JpaRepository<CitaMedica, Integer> {
    

    boolean existsByMedicoIdAndFechaCitaAndHoraCita(Integer medicoId, LocalDate fechaCita, LocalTime horaCita);
    
    boolean existsByMedicoIdAndFechaCitaAndHoraCitaAndEstadoCitaNot(
    Integer medicoId, LocalDate fechaCita, LocalTime horaCita, EstadoCitaEnum estadoCita);

    boolean existsByPacienteIdAndFechaCitaAndHoraCitaAndEstadoCitaNot(
    Integer pacienteId, LocalDate fechaCita, LocalTime horaCita, EstadoCitaEnum estadoCita);

    boolean existsByPacienteIdAndFechaCitaAndHoraCita(Integer pacienteId, LocalDate fechaCita, LocalTime horaCita);
    
    List<CitaMedica> findAllByOrderByFechaCitaDescHoraCitaDesc();
    
    List<CitaMedica> findAllByMedicoIdOrderByFechaCitaDescHoraCitaDesc(Integer medicoId);

    List<CitaMedica> findByMedicoIdAndFechaCitaGreaterThanEqual(Integer medicoId, LocalDate fechaCita);
}
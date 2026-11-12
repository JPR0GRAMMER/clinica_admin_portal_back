package com.upn.gestion_clinica.repository;

import com.upn.gestion_clinica.entity.Paciente;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Paciente p where p.id = :id")
    Optional<Paciente> buscarParaActualizar(@Param("id") Integer id);
    
    Optional<Paciente> findByDocumentoIdentidad(String documentoIdentidad);
    
    boolean existsByDocumentoIdentidad(String documentoIdentidad);
}

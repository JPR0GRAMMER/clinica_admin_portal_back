package com.upn.gestion_clinica.repository;

import com.upn.gestion_clinica.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Integer> {
    
    Optional<Paciente> findByDocumentoIdentidad(String documentoIdentidad);
    
    boolean existsByDocumentoIdentidad(String documentoIdentidad);
}

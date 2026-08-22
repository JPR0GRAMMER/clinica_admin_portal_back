package com.upn.gestion_clinica.repository;

import com.upn.gestion_clinica.entity.Medico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicoRepository extends JpaRepository<Medico, Integer> {
    boolean existsByNumeroColegiatura(String numeroColegiatura);
    Optional<Medico> findByUsuarioId(Integer usuarioId);
    Optional<Medico> findByUsuarioCorreo(String correo);
}

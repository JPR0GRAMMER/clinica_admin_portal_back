package com.upn.gestion_clinica.repository;

import com.upn.gestion_clinica.entity.Farmaceutico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FarmaceuticoRepository extends JpaRepository<Farmaceutico, Integer> {
    Optional<Farmaceutico> findByUsuarioId(Integer usuarioId);
    boolean existsByNumeroColegiatura(String numeroColegiatura);
}

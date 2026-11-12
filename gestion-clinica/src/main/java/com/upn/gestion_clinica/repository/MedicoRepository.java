package com.upn.gestion_clinica.repository;

import com.upn.gestion_clinica.entity.Medico;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MedicoRepository extends JpaRepository<Medico, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Medico m where m.id = :id")
    Optional<Medico> buscarParaActualizar(@Param("id") Integer id);
    boolean existsByNumeroColegiatura(String numeroColegiatura);
    Optional<Medico> findByUsuarioId(Integer usuarioId);
    Optional<Medico> findByUsuarioCorreo(String correo);
}

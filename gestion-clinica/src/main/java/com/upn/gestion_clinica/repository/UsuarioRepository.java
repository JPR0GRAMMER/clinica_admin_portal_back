package com.upn.gestion_clinica.repository;

import com.upn.gestion_clinica.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    boolean existsByCorreo(String correo);
    Optional<Usuario> findByCorreo(String correo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.correo = :correo")
    Optional<Usuario> findByCorreoForUpdate(@Param("correo") String correo);

    @Query("SELECT u FROM Usuario u WHERE u.rol.estado = 1")
    List<Usuario> findAllConRolActivo();
}

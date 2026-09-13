package com.upn.gestion_clinica.repository;

import com.upn.gestion_clinica.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Integer> {
    Optional<Rol> findByNombre(String nombre);
    List<Rol> findByEstado(Integer estado);
}

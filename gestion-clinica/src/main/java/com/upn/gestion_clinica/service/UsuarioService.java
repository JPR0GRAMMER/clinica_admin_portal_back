package com.upn.gestion_clinica.service;

import com.upn.gestion_clinica.dto.usuario.UsuarioActualizarDto;
import com.upn.gestion_clinica.dto.usuario.UsuarioRegistroDto;
import com.upn.gestion_clinica.dto.usuario.UsuarioResponseDto;

import java.util.List;

public interface UsuarioService {
    
    UsuarioResponseDto registrarUsuario(UsuarioRegistroDto dto);

    List<UsuarioResponseDto> listarUsuarios();
    UsuarioResponseDto obtenerUsuario(Integer id);
    UsuarioResponseDto actualizarUsuario(Integer id, UsuarioActualizarDto dto);
    void cambiarEstado(Integer id, Integer estado);
}

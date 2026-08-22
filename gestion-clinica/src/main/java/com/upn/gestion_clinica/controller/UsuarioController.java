package com.upn.gestion_clinica.controller;

import com.upn.gestion_clinica.dto.usuario.UsuarioActualizarDto;
import com.upn.gestion_clinica.dto.usuario.UsuarioRegistroDto;
import com.upn.gestion_clinica.dto.usuario.UsuarioResponseDto;
import com.upn.gestion_clinica.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDto> registrarUsuario(@Valid @RequestBody UsuarioRegistroDto dto) {
        UsuarioResponseDto responseDto = usuarioService.registrarUsuario(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDto>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDto> obtenerUsuario(@PathVariable Integer id) {
        return ResponseEntity.ok(usuarioService.obtenerUsuario(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDto> actualizarUsuario(@PathVariable Integer id, @Valid @RequestBody UsuarioActualizarDto dto) {
        return ResponseEntity.ok(usuarioService.actualizarUsuario(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivarUsuario(@PathVariable Integer id) {
        usuarioService.cambiarEstado(id, 0);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activar")
    public ResponseEntity<Void> activarUsuario(@PathVariable Integer id) {
        usuarioService.cambiarEstado(id, 1);
        return ResponseEntity.noContent().build();
    }
}

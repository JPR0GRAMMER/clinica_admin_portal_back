package com.upn.gestion_clinica.controller;

import com.upn.gestion_clinica.dto.AuthResponseDto;
import com.upn.gestion_clinica.dto.LoginRequestDto;
import com.upn.gestion_clinica.security.CustomUserDetailsService;
import com.upn.gestion_clinica.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.upn.gestion_clinica.repository.UsuarioRepository;
import com.upn.gestion_clinica.entity.Usuario;
import com.upn.gestion_clinica.dto.VistaDto;
import java.util.stream.Collectors;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;

    public AuthController(AuthenticationManager authenticationManager, CustomUserDetailsService userDetailsService, JwtUtil jwtUtil, UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getContrasena())
        );

        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getCorreo());
        final String jwt = jwtUtil.generateToken(userDetails);
        
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo()).orElseThrow();
        String rol = usuario.getRol().getNombre();
        List<VistaDto> vistas = usuario.getRol().getVistas().stream()
            .filter(v -> v.getEstado() == 1)
            .map(v -> {
                VistaDto dto = new VistaDto();
                dto.setNombre(v.getNombre());
                dto.setRuta(v.getRuta());
                dto.setIcono(v.getIcono());
                return dto;
            }).collect(Collectors.toList());

        return ResponseEntity.ok(new AuthResponseDto(jwt, rol, vistas));
    }
}

package com.upn.gestion_clinica.security;

import com.upn.gestion_clinica.entity.Usuario;
import com.upn.gestion_clinica.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class LoginAttemptService {

    public static final int MAX_FAILED_ATTEMPTS = 3;
    public static final String ACCOUNT_LOCKED_MESSAGE =
            "La cuenta fue bloqueada por superar 3 intentos fallidos. Contacta al administrador.";

    private final UsuarioRepository usuarioRepository;

    public LoginAttemptService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public boolean isBlocked(String correo) {
        if (correo == null || correo.isBlank()) {
            return false;
        }
        return usuarioRepository.findByCorreo(correo)
                .map(u -> Integer.valueOf(1).equals(u.getEstado()) && Boolean.TRUE.equals(u.getBloqueado()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public int getRemainingAttempts(String correo) {
        if (correo == null || correo.isBlank()) {
            return MAX_FAILED_ATTEMPTS;
        }
        return usuarioRepository.findByCorreo(correo)
                .map(u -> Math.max(0, MAX_FAILED_ATTEMPTS - Optional.ofNullable(u.getIntentosFallidos()).orElse(0)))
                .orElse(MAX_FAILED_ATTEMPTS);
    }

    public String buildFailureMessage(String correo) {
        int restantes = getRemainingAttempts(correo);
        if (restantes == 1) {
            return "Credenciales incorrectas. Te queda 1 intento antes de que tu cuenta sea bloqueada.";
        }
        if (restantes > 1 && restantes < MAX_FAILED_ATTEMPTS) {
            return "Credenciales incorrectas. Te quedan " + restantes + " intentos antes de que tu cuenta sea bloqueada.";
        }
        return "Credenciales incorrectas. Recuerda que tras 3 intentos fallidos la cuenta será bloqueada.";
    }

    @Transactional
    public boolean registerFailure(String correo) {
        Optional<Usuario> usuarioOptional = usuarioRepository.findByCorreoForUpdate(correo);
        if (usuarioOptional.isEmpty()) {
            return false;
        }

        Usuario usuario = usuarioOptional.get();
        if (!Integer.valueOf(1).equals(usuario.getEstado())) {
            return false;
        }

        if (Boolean.TRUE.equals(usuario.getBloqueado())) {
            return true;
        }

        int intentosFallidos = Optional.ofNullable(usuario.getIntentosFallidos()).orElse(0) + 1;
        usuario.setIntentosFallidos(intentosFallidos);
        if (intentosFallidos >= MAX_FAILED_ATTEMPTS) {
            usuario.setBloqueado(true);
        }
        usuarioRepository.save(usuario);

        return Boolean.TRUE.equals(usuario.getBloqueado());
    }

    @Transactional
    public void resetAfterSuccessfulLogin(String correo) {
        usuarioRepository.findByCorreoForUpdate(correo).ifPresent(usuario -> {
            if (Integer.valueOf(1).equals(usuario.getEstado())
                    && !Boolean.TRUE.equals(usuario.getBloqueado())
                    && Optional.ofNullable(usuario.getIntentosFallidos()).orElse(0) > 0) {
                usuario.setIntentosFallidos(0);
                usuarioRepository.save(usuario);
            }
        });
    }
}

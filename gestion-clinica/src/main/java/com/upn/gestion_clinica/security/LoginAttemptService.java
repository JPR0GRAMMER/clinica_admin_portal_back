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

    @Transactional
    public boolean registerFailure(String correo) {
        Optional<Usuario> usuarioOptional = usuarioRepository.findByCorreoForUpdate(correo);
        if (usuarioOptional.isEmpty()) {
            return false;
        }

        Usuario usuario = usuarioOptional.get();
        if (!Integer.valueOf(1).equals(usuario.getEstado()) || Boolean.TRUE.equals(usuario.getBloqueado())) {
            return false;
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

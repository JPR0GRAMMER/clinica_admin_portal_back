package com.upn.gestion_clinica.security;

import com.upn.gestion_clinica.entity.Usuario;
import com.upn.gestion_clinica.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private LoginAttemptService loginAttemptService;

    @Test
    @DisplayName("LoginAttemptService.registerFailure() - incrementa intentos sin bloquear antes del límite")
    void registerFailure_IncrementaIntentosAntesDelLimite() {
        Usuario usuario = usuario(0, false);
        when(usuarioRepository.findByCorreoForUpdate("usuario@clinica.com")).thenReturn(Optional.of(usuario));

        boolean bloqueada = loginAttemptService.registerFailure("usuario@clinica.com");

        assertFalse(bloqueada);
        assertEquals(1, usuario.getIntentosFallidos());
        assertFalse(usuario.getBloqueado());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("LoginAttemptService.registerFailure() - bloquea al tercer intento fallido")
    void registerFailure_BloqueaAlTercerIntento() {
        Usuario usuario = usuario(2, false);
        when(usuarioRepository.findByCorreoForUpdate("usuario@clinica.com")).thenReturn(Optional.of(usuario));

        boolean bloqueada = loginAttemptService.registerFailure("usuario@clinica.com");

        assertTrue(bloqueada);
        assertEquals(3, usuario.getIntentosFallidos());
        assertTrue(usuario.getBloqueado());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("LoginAttemptService.registerFailure() - tres intentos consecutivos bloquean la cuenta")
    void registerFailure_TresIntentosConsecutivosBloqueanLaCuenta() {
        Usuario usuario = usuario(0, false);
        when(usuarioRepository.findByCorreoForUpdate("usuario@clinica.com")).thenReturn(Optional.of(usuario));

        assertFalse(loginAttemptService.registerFailure("usuario@clinica.com"));
        assertFalse(loginAttemptService.registerFailure("usuario@clinica.com"));
        assertTrue(loginAttemptService.registerFailure("usuario@clinica.com"));

        assertEquals(3, usuario.getIntentosFallidos());
        assertTrue(usuario.getBloqueado());
        verify(usuarioRepository, times(3)).save(usuario);
    }

    @Test
    @DisplayName("LoginAttemptService.registerFailure() - no incrementa una cuenta ya bloqueada")
    void registerFailure_NoIncrementaCuentaBloqueada() {
        Usuario usuario = usuario(3, true);
        when(usuarioRepository.findByCorreoForUpdate("usuario@clinica.com")).thenReturn(Optional.of(usuario));

        boolean bloqueada = loginAttemptService.registerFailure("usuario@clinica.com");

        assertFalse(bloqueada);
        assertEquals(3, usuario.getIntentosFallidos());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("LoginAttemptService.resetAfterSuccessfulLogin() - reinicia el contador")
    void resetAfterSuccessfulLogin_ReiniciaContador() {
        Usuario usuario = usuario(2, false);
        when(usuarioRepository.findByCorreoForUpdate("usuario@clinica.com")).thenReturn(Optional.of(usuario));

        loginAttemptService.resetAfterSuccessfulLogin("usuario@clinica.com");

        assertEquals(0, usuario.getIntentosFallidos());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("LoginAttemptService.registerFailure() - no cuenta usuarios inactivos")
    void registerFailure_NoCuentaUsuarioInactivo() {
        Usuario usuario = usuario(0, false);
        usuario.setEstado(0);
        when(usuarioRepository.findByCorreoForUpdate("usuario@clinica.com")).thenReturn(Optional.of(usuario));

        boolean bloqueada = loginAttemptService.registerFailure("usuario@clinica.com");

        assertFalse(bloqueada);
        assertEquals(0, usuario.getIntentosFallidos());
        verify(usuarioRepository, never()).save(any());
    }

    private Usuario usuario(int intentosFallidos, boolean bloqueado) {
        Usuario usuario = new Usuario();
        usuario.setEstado(1);
        usuario.setIntentosFallidos(intentosFallidos);
        usuario.setBloqueado(bloqueado);
        return usuario;
    }
}

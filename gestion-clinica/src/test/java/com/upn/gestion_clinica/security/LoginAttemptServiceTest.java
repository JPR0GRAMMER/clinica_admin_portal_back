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
    @DisplayName("LoginAttemptService.registerFailure() - no incrementa una cuenta ya bloqueada y retorna true")
    void registerFailure_NoIncrementaCuentaBloqueada() {
        Usuario usuario = usuario(3, true);
        when(usuarioRepository.findByCorreoForUpdate("usuario@clinica.com")).thenReturn(Optional.of(usuario));

        boolean bloqueada = loginAttemptService.registerFailure("usuario@clinica.com");

        assertTrue(bloqueada);
        assertEquals(3, usuario.getIntentosFallidos());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("LoginAttemptService.isBlocked() - retorna true cuando la cuenta está activa y bloqueada")
    void isBlocked_RetornaTrueCuandoCuentaBloqueada() {
        Usuario usuario = usuario(3, true);
        when(usuarioRepository.findByCorreo("usuario@clinica.com")).thenReturn(Optional.of(usuario));

        assertTrue(loginAttemptService.isBlocked("usuario@clinica.com"));
    }

    @Test
    @DisplayName("LoginAttemptService.isBlocked() - retorna false cuando la cuenta no está bloqueada")
    void isBlocked_RetornaFalseCuandoCuentaNoBloqueada() {
        Usuario usuario = usuario(1, false);
        when(usuarioRepository.findByCorreo("usuario@clinica.com")).thenReturn(Optional.of(usuario));

        assertFalse(loginAttemptService.isBlocked("usuario@clinica.com"));
    }

    @Test
    @DisplayName("LoginAttemptService.isBlocked() - retorna false para correos no encontrados o nulos")
    void isBlocked_RetornaFalseParaNoEncontradoONulo() {
        assertFalse(loginAttemptService.isBlocked(null));
        assertFalse(loginAttemptService.isBlocked("   "));

        when(usuarioRepository.findByCorreo("desconocido@clinica.com")).thenReturn(Optional.empty());
        assertFalse(loginAttemptService.isBlocked("desconocido@clinica.com"));
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

    @Test
    @DisplayName("LoginAttemptService.buildFailureMessage() - formatea mensaje plural para 2 intentos restantes")
    void buildFailureMessage_PluralParaDosIntentos() {
        Usuario usuario = usuario(1, false);
        when(usuarioRepository.findByCorreo("usuario@clinica.com")).thenReturn(Optional.of(usuario));

        String mensaje = loginAttemptService.buildFailureMessage("usuario@clinica.com");

        assertEquals("Credenciales incorrectas. Te quedan 2 intentos antes de que tu cuenta sea bloqueada.", mensaje);
    }

    @Test
    @DisplayName("LoginAttemptService.buildFailureMessage() - formatea mensaje singular para 1 intento restante")
    void buildFailureMessage_SingularParaUnIntento() {
        Usuario usuario = usuario(2, false);
        when(usuarioRepository.findByCorreo("usuario@clinica.com")).thenReturn(Optional.of(usuario));

        String mensaje = loginAttemptService.buildFailureMessage("usuario@clinica.com");

        assertEquals("Credenciales incorrectas. Te queda 1 intento antes de que tu cuenta sea bloqueada.", mensaje);
    }

    @Test
    @DisplayName("LoginAttemptService.buildFailureMessage() - mensaje por defecto para usuario no encontrado")
    void buildFailureMessage_MensajePorDefecto() {
        when(usuarioRepository.findByCorreo("desconocido@clinica.com")).thenReturn(Optional.empty());

        String mensaje = loginAttemptService.buildFailureMessage("desconocido@clinica.com");

        assertEquals("Credenciales incorrectas. Recuerda que tras 3 intentos fallidos la cuenta será bloqueada.", mensaje);
    }

    private Usuario usuario(int intentosFallidos, boolean bloqueado) {
        Usuario usuario = new Usuario();
        usuario.setEstado(1);
        usuario.setIntentosFallidos(intentosFallidos);
        usuario.setBloqueado(bloqueado);
        return usuario;
    }
}

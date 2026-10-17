package com.upn.gestion_clinica.controller;

import com.upn.gestion_clinica.repository.UsuarioRepository;
import com.upn.gestion_clinica.security.CustomUserDetailsService;
import com.upn.gestion_clinica.security.JwtUtil;
import com.upn.gestion_clinica.security.LoginAttemptService;
import com.upn.gestion_clinica.dto.LoginRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("AuthController.login() - bloquea al alcanzar el tercer intento fallido")
    void login_BloqueaAlAlcanzarElTercerIntentoFallido() {
        LoginRequestDto request = request();
        BadCredentialsException authenticationFailure = new BadCredentialsException("Credenciales inválidas");
        when(authenticationManager.authenticate(any())).thenThrow(authenticationFailure);
        when(loginAttemptService.registerFailure(request.getCorreo())).thenReturn(true);

        LockedException exception = assertThrows(LockedException.class,
                () -> authController.login(request));

        assertEquals(LoginAttemptService.ACCOUNT_LOCKED_MESSAGE, exception.getMessage());
        verify(loginAttemptService).registerFailure(request.getCorreo());
    }

    @Test
    @DisplayName("AuthController.login() - advierte intentos restantes antes de alcanzar el límite")
    void login_AdvierteIntentosRestantesAntesDelLimite() {
        LoginRequestDto request = request();
        BadCredentialsException authenticationFailure = new BadCredentialsException("Credenciales inválidas");
        when(authenticationManager.authenticate(any())).thenThrow(authenticationFailure);
        when(loginAttemptService.registerFailure(request.getCorreo())).thenReturn(false);
        when(loginAttemptService.buildFailureMessage(request.getCorreo()))
                .thenReturn("Credenciales incorrectas. Te quedan 2 intentos antes de que tu cuenta sea bloqueada.");

        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> authController.login(request));

        assertEquals("Credenciales incorrectas. Te quedan 2 intentos antes de que tu cuenta sea bloqueada.", exception.getMessage());
        verify(loginAttemptService).registerFailure(request.getCorreo());
        verify(loginAttemptService).buildFailureMessage(request.getCorreo());
    }

    @Test
    @DisplayName("AuthController.login() - rechaza directamente si la cuenta ya se encuentra bloqueada")
    void login_RechazaDirectamenteSiCuentaYaBloqueada() {
        LoginRequestDto request = request();
        when(loginAttemptService.isBlocked(request.getCorreo())).thenReturn(true);

        LockedException exception = assertThrows(LockedException.class,
                () -> authController.login(request));

        assertEquals(LoginAttemptService.ACCOUNT_LOCKED_MESSAGE, exception.getMessage());
        verify(authenticationManager, never()).authenticate(any());
    }

    private LoginRequestDto request() {
        LoginRequestDto request = new LoginRequestDto();
        request.setCorreo("usuario@clinica.com");
        request.setContrasena("incorrecta");
        return request;
    }
}

package com.upn.gestion_clinica.security;

import com.upn.gestion_clinica.entity.Rol;
import com.upn.gestion_clinica.entity.Usuario;
import com.upn.gestion_clinica.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.LockedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("CustomUserDetailsService.loadUserByUsername() - rechaza cuenta bloqueada")
    void loadUserByUsername_RechazaCuentaBloqueada() {
        Usuario usuario = new Usuario();
        usuario.setCorreo("usuario@clinica.com");
        usuario.setEstado(1);
        usuario.setBloqueado(true);
        when(usuarioRepository.findByCorreo("usuario@clinica.com")).thenReturn(Optional.of(usuario));

        LockedException exception = assertThrows(LockedException.class,
                () -> userDetailsService.loadUserByUsername("usuario@clinica.com"));

        assertEquals(LoginAttemptService.ACCOUNT_LOCKED_MESSAGE, exception.getMessage());
    }

    @Test
    @DisplayName("CustomUserDetailsService.loadUserByUsername() - mantiene autenticación activa para cuenta no bloqueada")
    void loadUserByUsername_AceptaCuentaNoBloqueada() {
        Rol rol = new Rol();
        rol.setNombre("MEDICO");
        Usuario usuario = new Usuario();
        usuario.setCorreo("usuario@clinica.com");
        usuario.setContrasena("hash");
        usuario.setEstado(1);
        usuario.setBloqueado(false);
        usuario.setRol(rol);
        when(usuarioRepository.findByCorreo("usuario@clinica.com")).thenReturn(Optional.of(usuario));

        var details = userDetailsService.loadUserByUsername("usuario@clinica.com");

        assertEquals("usuario@clinica.com", details.getUsername());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MEDICO")));
    }
}

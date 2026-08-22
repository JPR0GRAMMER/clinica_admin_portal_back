package com.upn.gestion_clinica.security;

import com.upn.gestion_clinica.entity.Usuario;
import com.upn.gestion_clinica.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con correo: " + username));

        if (usuario.getEstado() == 0) {
            throw new UsernameNotFoundException("El usuario está inactivo.");
        }

        String roleName = "ROLE_" + usuario.getRol().getNombre().toUpperCase();
        
        return new org.springframework.security.core.userdetails.User(
                usuario.getCorreo(),
                usuario.getContrasena(),
                Collections.singletonList(new SimpleGrantedAuthority(roleName))
        );
    }
}

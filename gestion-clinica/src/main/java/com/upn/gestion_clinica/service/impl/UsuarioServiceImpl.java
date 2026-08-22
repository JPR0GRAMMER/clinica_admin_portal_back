package com.upn.gestion_clinica.service.impl;

import com.upn.gestion_clinica.dto.usuario.DetallesMedicoDto;
import com.upn.gestion_clinica.dto.usuario.UsuarioActualizarDto;
import com.upn.gestion_clinica.dto.usuario.UsuarioRegistroDto;
import com.upn.gestion_clinica.dto.usuario.UsuarioResponseDto;
import com.upn.gestion_clinica.dto.usuario.DetallesFarmaceuticoDto;
import com.upn.gestion_clinica.entity.Rol;
import com.upn.gestion_clinica.entity.Usuario;
import com.upn.gestion_clinica.entity.Especialidad;
import com.upn.gestion_clinica.entity.Medico;
import com.upn.gestion_clinica.entity.Farmaceutico;
import com.upn.gestion_clinica.repository.EspecialidadRepository;
import com.upn.gestion_clinica.repository.MedicoRepository;
import com.upn.gestion_clinica.repository.FarmaceuticoRepository;
import com.upn.gestion_clinica.repository.RolRepository;
import com.upn.gestion_clinica.repository.UsuarioRepository;
import com.upn.gestion_clinica.service.UsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final EspecialidadRepository especialidadRepository;
    private final MedicoRepository medicoRepository;
    private final FarmaceuticoRepository farmaceuticoRepository;

    UsuarioServiceImpl(UsuarioRepository usuarioRepository, RolRepository rolRepository,
            PasswordEncoder passwordEncoder, EspecialidadRepository especialidadRepository,
            MedicoRepository medicoRepository, FarmaceuticoRepository farmaceuticoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.especialidadRepository = especialidadRepository;
        this.medicoRepository = medicoRepository;
        this.farmaceuticoRepository = farmaceuticoRepository;
    }

    @Override
    @Transactional
    public UsuarioResponseDto registrarUsuario(UsuarioRegistroDto dto) {
        Rol rol = validarCorreoYObtenerRol(dto.getCorreo(), dto.getRolId());

        if (rol.getNombre().equalsIgnoreCase("MEDICO")) {
            if (dto.getDetallesMedico() == null) {
                throw new IllegalArgumentException("Los detalles del médico son obligatorios.");
            }
            if (medicoRepository.existsByNumeroColegiatura(dto.getDetallesMedico().getNumeroColegiatura())) {
                throw new IllegalArgumentException("El número de colegiatura ya se encuentra registrado.");
            }
            Especialidad especialidad = especialidadRepository.findById(dto.getDetallesMedico().getEspecialidadId())
                    .orElseThrow(() -> new IllegalArgumentException("La especialidad especificada no existe."));

            Usuario usuarioGuardado = crearYGuardarUsuarioBase(dto, rol);

            Medico medico = new Medico();
            medico.setUsuario(usuarioGuardado);
            medico.setEspecialidad(especialidad);
            medico.setNumeroColegiatura(dto.getDetallesMedico().getNumeroColegiatura());
            medicoRepository.save(medico);

            return mapToResponseDto(usuarioGuardado);
        } else if (rol.getNombre().equalsIgnoreCase("FARMACEUTICO")) {
            if (dto.getDetallesFarmaceutico() == null) {
                throw new IllegalArgumentException("Los detalles del farmacéutico son obligatorios.");
            }
            if (farmaceuticoRepository.existsByNumeroColegiatura(dto.getDetallesFarmaceutico().getNumeroColegiatura())) {
                throw new IllegalArgumentException("El número de colegiatura (CQFP) ya se encuentra registrado.");
            }

            Usuario usuarioGuardado = crearYGuardarUsuarioBase(dto, rol);

            Farmaceutico farmaceutico = new Farmaceutico();
            farmaceutico.setUsuario(usuarioGuardado);
            farmaceutico.setNumeroColegiatura(dto.getDetallesFarmaceutico().getNumeroColegiatura());
            farmaceuticoRepository.save(farmaceutico);

            return mapToResponseDto(usuarioGuardado);
        } else {
            Usuario usuarioGuardado = crearYGuardarUsuarioBase(dto, rol);
            return mapToResponseDto(usuarioGuardado);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponseDto> listarUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDto obtenerUsuario(Integer id) {
        return mapToResponseDto(buscarUsuarioPorId(id));
    }

    @Override
    @Transactional
    public UsuarioResponseDto actualizarUsuario(Integer id, UsuarioActualizarDto dto) {
        Usuario usuario = buscarUsuarioPorId(id);
        
        if (!usuario.getCorreo().equalsIgnoreCase(dto.getCorreo())) {
            if (usuarioRepository.existsByCorreo(dto.getCorreo())) {
                throw new IllegalArgumentException("El correo ya se encuentra registrado por otro usuario.");
            }
            usuario.setCorreo(dto.getCorreo());
        }
        
        if (dto.getContrasena() != null && !dto.getContrasena().trim().isEmpty()) {
            usuario.setContrasena(passwordEncoder.encode(dto.getContrasena()));
        }
        
        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        
        if ("MEDICO".equalsIgnoreCase(usuario.getRol().getNombre()) && dto.getDetallesMedico() != null) {
            Medico medico = medicoRepository.findByUsuarioId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Datos médicos no encontrados."));
                    
            if (!medico.getNumeroColegiatura().equals(dto.getDetallesMedico().getNumeroColegiatura()) && 
                 medicoRepository.existsByNumeroColegiatura(dto.getDetallesMedico().getNumeroColegiatura())) {
                 throw new IllegalArgumentException("El número de colegiatura ya se encuentra registrado por otro médico.");
            }
            
            Especialidad especialidad = especialidadRepository.findById(dto.getDetallesMedico().getEspecialidadId())
                    .orElseThrow(() -> new IllegalArgumentException("La especialidad especificada no existe."));
                    
            medico.setEspecialidad(especialidad);
            medico.setNumeroColegiatura(dto.getDetallesMedico().getNumeroColegiatura());
            medicoRepository.save(medico);
        } else if ("FARMACEUTICO".equalsIgnoreCase(usuario.getRol().getNombre()) && dto.getDetallesFarmaceutico() != null) {
            Farmaceutico farmaceutico = farmaceuticoRepository.findByUsuarioId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Datos farmacéuticos no encontrados."));
                    
            if (!farmaceutico.getNumeroColegiatura().equals(dto.getDetallesFarmaceutico().getNumeroColegiatura()) && 
                 farmaceuticoRepository.existsByNumeroColegiatura(dto.getDetallesFarmaceutico().getNumeroColegiatura())) {
                 throw new IllegalArgumentException("El número de colegiatura (CQFP) ya se encuentra registrado por otro farmacéutico.");
            }
            
            farmaceutico.setNumeroColegiatura(dto.getDetallesFarmaceutico().getNumeroColegiatura());
            farmaceuticoRepository.save(farmaceutico);
        }
        
        return mapToResponseDto(usuarioRepository.save(usuario));
    }



    @Override
    @Transactional
    public void cambiarEstado(Integer id, Integer estado) {
        Usuario usuario = buscarUsuarioPorId(id);
        usuario.setEstado(estado);
        usuarioRepository.save(usuario);
    }

    private Rol validarCorreoYObtenerRol(String correo, Integer rolId) {
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new IllegalArgumentException("El correo ya se encuentra registrado.");
        }
        return rolRepository.findById(rolId)
                .orElseThrow(() -> new IllegalArgumentException("El rol especificado no existe."));
    }

    private Usuario crearYGuardarUsuarioBase(UsuarioRegistroDto dto, Rol rol) {
        String contrasenaEncriptada = passwordEncoder.encode(dto.getContrasena());
        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        usuario.setCorreo(dto.getCorreo());
        usuario.setContrasena(contrasenaEncriptada);
        usuario.setRol(rol);
        usuario.setEstado(1);
        return usuarioRepository.save(usuario);
    }

    private Usuario buscarUsuarioPorId(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + id));
    }



    private UsuarioResponseDto mapToResponseDto(Usuario usuario) {
        UsuarioResponseDto response = new UsuarioResponseDto();
        response.setId(usuario.getId());
        response.setNombre(usuario.getNombre());
        response.setApellido(usuario.getApellido());
        response.setCorreo(usuario.getCorreo());
        response.setRolNombre(usuario.getRol().getNombre());
        response.setEstado(usuario.getEstado());
        
        if ("MEDICO".equalsIgnoreCase(usuario.getRol().getNombre())) {
            medicoRepository.findByUsuarioId(usuario.getId()).ifPresent(medico -> {
                DetallesMedicoDto detalles = new DetallesMedicoDto();
                detalles.setNumeroColegiatura(medico.getNumeroColegiatura());
                detalles.setEspecialidadNombre(medico.getEspecialidad().getNombre());
                response.setDetallesMedico(detalles);
            });
        } else if ("FARMACEUTICO".equalsIgnoreCase(usuario.getRol().getNombre())) {
            farmaceuticoRepository.findByUsuarioId(usuario.getId()).ifPresent(farmaceutico -> {
                DetallesFarmaceuticoDto detalles = new DetallesFarmaceuticoDto();
                detalles.setNumeroColegiatura(farmaceutico.getNumeroColegiatura());
                response.setDetallesFarmaceutico(detalles);
            });
        }
        
        return response;
    }
}

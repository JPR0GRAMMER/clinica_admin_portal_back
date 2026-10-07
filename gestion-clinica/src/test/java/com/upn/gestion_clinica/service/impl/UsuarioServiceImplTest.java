package com.upn.gestion_clinica.service.impl;

import com.upn.gestion_clinica.dto.usuario.DetallesFarmaceuticoDto;
import com.upn.gestion_clinica.dto.usuario.DetallesMedicoRegistroDto;
import com.upn.gestion_clinica.dto.usuario.UsuarioActualizarDto;
import com.upn.gestion_clinica.dto.usuario.UsuarioRegistroDto;
import com.upn.gestion_clinica.dto.usuario.UsuarioResponseDto;
import com.upn.gestion_clinica.entity.Especialidad;
import com.upn.gestion_clinica.entity.Farmaceutico;
import com.upn.gestion_clinica.entity.Medico;
import com.upn.gestion_clinica.entity.Rol;
import com.upn.gestion_clinica.entity.Usuario;
import com.upn.gestion_clinica.repository.EspecialidadRepository;
import com.upn.gestion_clinica.repository.FarmaceuticoRepository;
import com.upn.gestion_clinica.repository.MedicoRepository;
import com.upn.gestion_clinica.repository.RolRepository;
import com.upn.gestion_clinica.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EspecialidadRepository especialidadRepository;

    @Mock
    private MedicoRepository medicoRepository;

    @Mock
    private FarmaceuticoRepository farmaceuticoRepository;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private Rol rolAdministrador;
    private Rol rolMedico;
    private Rol rolFarmaceutico;
    private Especialidad especialidad;

    @BeforeEach
    void setUp() {
        rolAdministrador = rol(1, "ADMINISTRADOR");
        rolMedico = rol(2, "MEDICO");
        rolFarmaceutico = rol(3, "FARMACEUTICO");

        especialidad = new Especialidad();
        especialidad.setId(10);
        especialidad.setNombre("Medicina General");

        lenient().when(passwordEncoder.encode(any())).thenReturn("hash-seguro");
    }

    @Test
    @DisplayName("UsuarioServiceImpl.registrarUsuario() - registra un administrador")
    void registrarUsuario_RegistraAdministrador() {
        UsuarioRegistroDto request = registro("admin@clinica.com", 1);
        Usuario usuarioGuardado = usuario(100, request, rolAdministrador);
        when(usuarioRepository.existsByCorreo(request.getCorreo())).thenReturn(false);
        when(rolRepository.findById(1)).thenReturn(Optional.of(rolAdministrador));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);

        UsuarioResponseDto response = usuarioService.registrarUsuario(request);

        assertEquals(100, response.getId());
        assertEquals("admin@clinica.com", response.getCorreo());
        assertEquals("ADMINISTRADOR", response.getRolNombre());
        assertEquals(1, response.getEstado());
        verify(usuarioRepository).save(any(Usuario.class));
        verifyNoInteractions(medicoRepository, farmaceuticoRepository);
    }

    @Test
    @DisplayName("UsuarioServiceImpl.registrarUsuario() - registra un médico con especialidad")
    void registrarUsuario_RegistraMedico() {
        UsuarioRegistroDto request = registro("medico@clinica.com", 2);
        DetallesMedicoRegistroDto detalles = new DetallesMedicoRegistroDto();
        detalles.setNumeroColegiatura("CMP-123");
        detalles.setEspecialidadId(10);
        request.setDetallesMedico(detalles);

        Usuario usuarioGuardado = usuario(101, request, rolMedico);
        Medico medico = new Medico();
        medico.setId(201);
        medico.setUsuario(usuarioGuardado);
        medico.setEspecialidad(especialidad);
        medico.setNumeroColegiatura("CMP-123");

        when(usuarioRepository.existsByCorreo(request.getCorreo())).thenReturn(false);
        when(rolRepository.findById(2)).thenReturn(Optional.of(rolMedico));
        when(medicoRepository.existsByNumeroColegiatura("CMP-123")).thenReturn(false);
        when(especialidadRepository.findById(10)).thenReturn(Optional.of(especialidad));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);
        when(medicoRepository.findByUsuarioId(101)).thenReturn(Optional.of(medico));

        UsuarioResponseDto response = usuarioService.registrarUsuario(request);

        assertEquals("MEDICO", response.getRolNombre());
        assertNotNull(response.getDetallesMedico());
        assertEquals("CMP-123", response.getDetallesMedico().getNumeroColegiatura());
        verify(medicoRepository).save(any(Medico.class));
    }

    @Test
    @DisplayName("UsuarioServiceImpl.registrarUsuario() - registra un farmacéutico")
    void registrarUsuario_RegistraFarmaceutico() {
        UsuarioRegistroDto request = registro("farmacia@clinica.com", 3);
        DetallesFarmaceuticoDto detalles = new DetallesFarmaceuticoDto();
        detalles.setNumeroColegiatura("CQFP-456");
        request.setDetallesFarmaceutico(detalles);

        Usuario usuarioGuardado = usuario(102, request, rolFarmaceutico);
        Farmaceutico farmaceutico = new Farmaceutico();
        farmaceutico.setId(202);
        farmaceutico.setUsuario(usuarioGuardado);
        farmaceutico.setNumeroColegiatura("CQFP-456");

        when(usuarioRepository.existsByCorreo(request.getCorreo())).thenReturn(false);
        when(rolRepository.findById(3)).thenReturn(Optional.of(rolFarmaceutico));
        when(farmaceuticoRepository.existsByNumeroColegiatura("CQFP-456")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);
        when(farmaceuticoRepository.findByUsuarioId(102)).thenReturn(Optional.of(farmaceutico));

        UsuarioResponseDto response = usuarioService.registrarUsuario(request);

        assertEquals("FARMACEUTICO", response.getRolNombre());
        assertNotNull(response.getDetallesFarmaceutico());
        assertEquals("CQFP-456", response.getDetallesFarmaceutico().getNumeroColegiatura());
        verify(farmaceuticoRepository).save(any(Farmaceutico.class));
    }

    @Test
    @DisplayName("UsuarioServiceImpl.registrarUsuario() - rechaza correo duplicado")
    void registrarUsuario_RechazaCorreoDuplicado() {
        UsuarioRegistroDto request = registro("repetido@clinica.com", 1);
        when(usuarioRepository.existsByCorreo(request.getCorreo())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.registrarUsuario(request));

        assertEquals("El correo ya se encuentra registrado.", exception.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("UsuarioServiceImpl.registrarUsuario() - rechaza rol inexistente")
    void registrarUsuario_RechazaRolInexistente() {
        UsuarioRegistroDto request = registro("nuevo@clinica.com", 99);
        when(usuarioRepository.existsByCorreo(request.getCorreo())).thenReturn(false);
        when(rolRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> usuarioService.registrarUsuario(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("UsuarioServiceImpl.registrarUsuario() - exige detalles médicos")
    void registrarUsuario_ExigeDetallesMedicos() {
        UsuarioRegistroDto request = registro("medico@clinica.com", 2);
        when(usuarioRepository.existsByCorreo(request.getCorreo())).thenReturn(false);
        when(rolRepository.findById(2)).thenReturn(Optional.of(rolMedico));

        assertThrows(IllegalArgumentException.class, () -> usuarioService.registrarUsuario(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("UsuarioServiceImpl.registrarUsuario() - rechaza colegiatura médica duplicada")
    void registrarUsuario_RechazaColegiaturaMedicaDuplicada() {
        UsuarioRegistroDto request = registroMedico();
        when(usuarioRepository.existsByCorreo(request.getCorreo())).thenReturn(false);
        when(rolRepository.findById(2)).thenReturn(Optional.of(rolMedico));
        when(medicoRepository.existsByNumeroColegiatura("CMP-123")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> usuarioService.registrarUsuario(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("UsuarioServiceImpl.registrarUsuario() - rechaza especialidad inexistente")
    void registrarUsuario_RechazaEspecialidadInexistente() {
        UsuarioRegistroDto request = registroMedico();
        when(usuarioRepository.existsByCorreo(request.getCorreo())).thenReturn(false);
        when(rolRepository.findById(2)).thenReturn(Optional.of(rolMedico));
        when(medicoRepository.existsByNumeroColegiatura("CMP-123")).thenReturn(false);
        when(especialidadRepository.findById(10)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> usuarioService.registrarUsuario(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("UsuarioServiceImpl.registrarUsuario() - exige detalles farmacéuticos y colegiatura única")
    void registrarUsuario_ValidaDetallesFarmaceuticos() {
        UsuarioRegistroDto sinDetalles = registro("farmacia@clinica.com", 3);
        when(usuarioRepository.existsByCorreo(sinDetalles.getCorreo())).thenReturn(false);
        when(rolRepository.findById(3)).thenReturn(Optional.of(rolFarmaceutico));
        assertThrows(IllegalArgumentException.class, () -> usuarioService.registrarUsuario(sinDetalles));

        UsuarioRegistroDto duplicado = registro("farmacia2@clinica.com", 3);
        DetallesFarmaceuticoDto detalles = new DetallesFarmaceuticoDto();
        detalles.setNumeroColegiatura("CQFP-456");
        duplicado.setDetallesFarmaceutico(detalles);
        when(usuarioRepository.existsByCorreo(duplicado.getCorreo())).thenReturn(false);
        when(farmaceuticoRepository.existsByNumeroColegiatura("CQFP-456")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> usuarioService.registrarUsuario(duplicado));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("UsuarioServiceImpl.actualizarUsuario() - actualiza datos y contraseña")
    void actualizarUsuario_ActualizaDatosYContrasena() {
        Usuario usuario = usuario(10, "Ana", "Perez", "ana@clinica.com", rolAdministrador);
        UsuarioActualizarDto request = actualizar("ana.nueva@clinica.com", "Ana María", "Pérez", "Nueva123!");
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByCorreo(request.getCorreo())).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioResponseDto response = usuarioService.actualizarUsuario(10, request);

        assertEquals("ana.nueva@clinica.com", response.getCorreo());
        assertEquals("Ana María", response.getNombre());
        assertEquals("hash-seguro", usuario.getContrasena());
        verify(passwordEncoder).encode("Nueva123!");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("UsuarioServiceImpl.actualizarUsuario() - conserva contraseña vacía y correo actual")
    void actualizarUsuario_ConservaContrasenaCuandoVacia() {
        Usuario usuario = usuario(10, "Ana", "Perez", "ana@clinica.com", rolAdministrador);
        usuario.setContrasena("hash-anterior");
        UsuarioActualizarDto request = actualizar("ANA@CLINICA.COM", "Ana", "Pérez", "   ");
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        usuarioService.actualizarUsuario(10, request);

        assertEquals("hash-anterior", usuario.getContrasena());
        assertEquals("ana@clinica.com", usuario.getCorreo());
        verify(usuarioRepository, never()).existsByCorreo(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("UsuarioServiceImpl.actualizarUsuario() - rechaza correo duplicado")
    void actualizarUsuario_RechazaCorreoDuplicado() {
        Usuario usuario = usuario(10, "Ana", "Perez", "ana@clinica.com", rolAdministrador);
        UsuarioActualizarDto request = actualizar("otro@clinica.com", "Ana", "Pérez", null);
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByCorreo("otro@clinica.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> usuarioService.actualizarUsuario(10, request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("UsuarioServiceImpl.actualizarUsuario() - actualiza datos médicos")
    void actualizarUsuario_ActualizaDatosMedicos() {
        Usuario usuario = usuario(10, "Ana", "Perez", "ana@clinica.com", rolMedico);
        Medico medico = new Medico();
        medico.setId(20);
        medico.setUsuario(usuario);
        medico.setEspecialidad(especialidad);
        medico.setNumeroColegiatura("CMP-OLD");
        Especialidad nuevaEspecialidad = new Especialidad();
        nuevaEspecialidad.setId(11);
        nuevaEspecialidad.setNombre("Pediatría");
        UsuarioActualizarDto request = actualizar("ana@clinica.com", "Ana", "Pérez", null);
        DetallesMedicoRegistroDto detalles = new DetallesMedicoRegistroDto();
        detalles.setNumeroColegiatura("CMP-NEW");
        detalles.setEspecialidadId(11);
        request.setDetallesMedico(detalles);
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(usuario));
        when(medicoRepository.findByUsuarioId(10)).thenReturn(Optional.of(medico));
        when(medicoRepository.existsByNumeroColegiatura("CMP-NEW")).thenReturn(false);
        when(especialidadRepository.findById(11)).thenReturn(Optional.of(nuevaEspecialidad));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(medicoRepository.findByUsuarioId(10)).thenReturn(Optional.of(medico));

        usuarioService.actualizarUsuario(10, request);

        assertEquals("CMP-NEW", medico.getNumeroColegiatura());
        assertEquals(nuevaEspecialidad, medico.getEspecialidad());
        verify(medicoRepository).save(medico);
    }

    @Test
    @DisplayName("UsuarioServiceImpl.actualizarUsuario() - rechaza colegiatura médica duplicada")
    void actualizarUsuario_RechazaColegiaturaMedicaDuplicada() {
        Usuario usuario = usuario(10, "Ana", "Perez", "ana@clinica.com", rolMedico);
        Medico medico = new Medico();
        medico.setNumeroColegiatura("CMP-OLD");
        UsuarioActualizarDto request = actualizar("ana@clinica.com", "Ana", "Pérez", null);
        DetallesMedicoRegistroDto detalles = new DetallesMedicoRegistroDto();
        detalles.setNumeroColegiatura("CMP-NEW");
        detalles.setEspecialidadId(10);
        request.setDetallesMedico(detalles);
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(usuario));
        when(medicoRepository.findByUsuarioId(10)).thenReturn(Optional.of(medico));
        when(medicoRepository.existsByNumeroColegiatura("CMP-NEW")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> usuarioService.actualizarUsuario(10, request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("UsuarioServiceImpl.actualizarUsuario() - actualiza datos farmacéuticos")
    void actualizarUsuario_ActualizaDatosFarmaceuticos() {
        Usuario usuario = usuario(10, "Luis", "Soto", "luis@clinica.com", rolFarmaceutico);
        Farmaceutico farmaceutico = new Farmaceutico();
        farmaceutico.setNumeroColegiatura("CQFP-OLD");
        UsuarioActualizarDto request = actualizar("luis@clinica.com", "Luis", "Soto", null);
        DetallesFarmaceuticoDto detalles = new DetallesFarmaceuticoDto();
        detalles.setNumeroColegiatura("CQFP-NEW");
        request.setDetallesFarmaceutico(detalles);
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(usuario));
        when(farmaceuticoRepository.findByUsuarioId(10)).thenReturn(Optional.of(farmaceutico));
        when(farmaceuticoRepository.existsByNumeroColegiatura("CQFP-NEW")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        usuarioService.actualizarUsuario(10, request);

        assertEquals("CQFP-NEW", farmaceutico.getNumeroColegiatura());
        verify(farmaceuticoRepository).save(farmaceutico);
    }

    @Test
    @DisplayName("UsuarioServiceImpl.actualizarUsuario() - usuario inexistente")
    void actualizarUsuario_RechazaUsuarioInexistente() {
        when(usuarioRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> usuarioService.actualizarUsuario(999, actualizar("x@clinica.com", "X", "Y", null)));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("UsuarioServiceImpl.cambiarEstado() - activa y desactiva conservando el valor")
    void cambiarEstado_ActualizaValorPersistido() {
        Usuario usuario = usuario(10, "Ana", "Perez", "ana@clinica.com", rolAdministrador);
        java.util.List<Integer> estadosPersistidos = new java.util.ArrayList<>();
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(usuario));
        doAnswer(invocation -> {
            estadosPersistidos.add(invocation.<Usuario>getArgument(0).getEstado());
            return null;
        }).when(usuarioRepository).save(any(Usuario.class));

        usuarioService.cambiarEstado(10, 0);
        usuarioService.cambiarEstado(10, 1);

        verify(usuarioRepository, times(2)).save(usuario);
        assertEquals(java.util.List.of(0, 1), estadosPersistidos);
    }

    @Test
    @DisplayName("UsuarioServiceImpl.cambiarEstado() - usuario inexistente")
    void cambiarEstado_RechazaUsuarioInexistente() {
        when(usuarioRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> usuarioService.cambiarEstado(999, 1));
        verify(usuarioRepository, never()).save(any());
    }

    private UsuarioRegistroDto registro(String correo, Integer rolId) {
        UsuarioRegistroDto request = new UsuarioRegistroDto();
        request.setNombre("Nombre");
        request.setApellido("Apellido");
        request.setCorreo(correo);
        request.setContrasena("Clave123!");
        request.setRolId(rolId);
        return request;
    }

    private UsuarioRegistroDto registroMedico() {
        UsuarioRegistroDto request = registro("medico@clinica.com", 2);
        DetallesMedicoRegistroDto detalles = new DetallesMedicoRegistroDto();
        detalles.setNumeroColegiatura("CMP-123");
        detalles.setEspecialidadId(10);
        request.setDetallesMedico(detalles);
        return request;
    }

    private UsuarioActualizarDto actualizar(String correo, String nombre, String apellido, String contrasena) {
        UsuarioActualizarDto request = new UsuarioActualizarDto();
        request.setCorreo(correo);
        request.setNombre(nombre);
        request.setApellido(apellido);
        request.setContrasena(contrasena);
        return request;
    }

    private Usuario usuario(Integer id, UsuarioRegistroDto request, Rol rol) {
        return usuario(id, request.getNombre(), request.getApellido(), request.getCorreo(), rol);
    }

    private Usuario usuario(Integer id, String nombre, String apellido, String correo, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setCorreo(correo);
        usuario.setContrasena("hash-anterior");
        usuario.setRol(rol);
        usuario.setEstado(1);
        return usuario;
    }

    private Rol rol(Integer id, String nombre) {
        Rol rol = new Rol();
        rol.setId(id);
        rol.setNombre(nombre);
        rol.setEstado(1);
        return rol;
    }
}

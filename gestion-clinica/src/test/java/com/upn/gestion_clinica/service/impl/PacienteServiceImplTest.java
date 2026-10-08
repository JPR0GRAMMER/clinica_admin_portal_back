package com.upn.gestion_clinica.service.impl;

import com.upn.gestion_clinica.dto.paciente.PacienteRequestDto;
import com.upn.gestion_clinica.dto.paciente.PacienteResponseDto;
import com.upn.gestion_clinica.entity.Paciente;
import com.upn.gestion_clinica.repository.PacienteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PacienteServiceImplTest {

    @Mock
    private PacienteRepository pacienteRepository;

    @InjectMocks
    private PacienteServiceImpl pacienteService;

    @Test
    @DisplayName("PacienteServiceImpl.crear() - registra paciente y deja estado activo")
    void crear_RegistraPacienteConEstadoInicialActivo() {
        PacienteRequestDto request = request("72819283", "Carlos", "Mendoza");
        when(pacienteRepository.existsByDocumentoIdentidad("72819283")).thenReturn(false);
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> {
            Paciente paciente = invocation.getArgument(0);
            paciente.setId(1);
            return paciente;
        });

        PacienteResponseDto response = pacienteService.crear(request);

        assertEquals(1, response.getId());
        assertEquals("72819283", response.getDocumentoIdentidad());
        assertEquals("Carlos", response.getNombre());
        assertEquals("Mendoza", response.getApellido());
        assertEquals(request.getFechaNacimiento(), response.getFechaNacimiento());
        assertEquals(request.getTelefono(), response.getTelefono());
        assertEquals(request.getCorreo(), response.getCorreo());
        assertEquals(1, response.getEstado());
        verify(pacienteRepository).save(any(Paciente.class));
    }

    @Test
    @DisplayName("PacienteServiceImpl.crear() - rechaza documento de identidad duplicado")
    void crear_RechazaDocumentoDuplicado() {
        PacienteRequestDto request = request("72819283", "Carlos", "Mendoza");
        when(pacienteRepository.existsByDocumentoIdentidad("72819283")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> pacienteService.crear(request));

        assertTrue(exception.getMessage().contains("72819283"));
        verify(pacienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("PacienteServiceImpl.actualizar() - actualiza todos los campos")
    void actualizar_ActualizaCamposPaciente() {
        Paciente paciente = paciente(7, "72819283", "Carlos", "Mendoza");
        PacienteRequestDto request = request("87654321", "Carlos Alberto", "Mendoza López");
        when(pacienteRepository.findById(7)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.existsByDocumentoIdentidad("87654321")).thenReturn(false);
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PacienteResponseDto response = pacienteService.actualizar(7, request);

        assertEquals("87654321", response.getDocumentoIdentidad());
        assertEquals("Carlos Alberto", response.getNombre());
        assertEquals("Mendoza López", response.getApellido());
        assertEquals(request.getFechaNacimiento(), response.getFechaNacimiento());
        assertEquals(request.getTelefono(), response.getTelefono());
        assertEquals(request.getCorreo(), response.getCorreo());
        verify(pacienteRepository).save(paciente);
    }

    @Test
    @DisplayName("PacienteServiceImpl.actualizar() - paciente inexistente")
    void actualizar_RechazaPacienteInexistente() {
        when(pacienteRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> pacienteService.actualizar(999, request("87654321", "Carlos", "Mendoza")));
        verify(pacienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("PacienteServiceImpl.actualizar() - rechaza cambio a documento duplicado")
    void actualizar_RechazaNuevoDocumentoDuplicado() {
        Paciente paciente = paciente(7, "72819283", "Carlos", "Mendoza");
        when(pacienteRepository.findById(7)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.existsByDocumentoIdentidad("87654321")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> pacienteService.actualizar(7, request("87654321", "Carlos", "Mendoza")));
        verify(pacienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("PacienteServiceImpl.actualizar() - conserva documento actual sin validar duplicidad")
    void actualizar_ConservaDocumentoActual() {
        Paciente paciente = paciente(7, "72819283", "Carlos", "Mendoza");
        PacienteRequestDto request = request("72819283", "Carlos actualizado", "Mendoza");
        when(pacienteRepository.findById(7)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pacienteService.actualizar(7, request);

        assertEquals("72819283", paciente.getDocumentoIdentidad());
        assertEquals("Carlos actualizado", paciente.getNombre());
        verify(pacienteRepository, never()).existsByDocumentoIdentidad(any());
        verify(pacienteRepository).save(paciente);
    }

    private PacienteRequestDto request(String documento, String nombre, String apellido) {
        PacienteRequestDto request = new PacienteRequestDto();
        request.setDocumentoIdentidad(documento);
        request.setNombre(nombre);
        request.setApellido(apellido);
        request.setFechaNacimiento(LocalDate.of(1990, 5, 10));
        request.setTelefono("999888777");
        request.setCorreo("carlos@clinica.com");
        return request;
    }

    private Paciente paciente(Integer id, String documento, String nombre, String apellido) {
        Paciente paciente = new Paciente();
        paciente.setId(id);
        paciente.setDocumentoIdentidad(documento);
        paciente.setNombre(nombre);
        paciente.setApellido(apellido);
        paciente.setFechaNacimiento(LocalDate.of(1988, 4, 20));
        paciente.setTelefono("999111222");
        paciente.setCorreo("paciente@clinica.com");
        paciente.setEstado(1);
        return paciente;
    }
}

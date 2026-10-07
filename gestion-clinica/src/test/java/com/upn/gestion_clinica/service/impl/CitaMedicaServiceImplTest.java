package com.upn.gestion_clinica.service.impl;

import com.upn.gestion_clinica.dto.cita.CitaMedicaRequestDto;
import com.upn.gestion_clinica.dto.cita.CitaMedicaResponseDto;
import com.upn.gestion_clinica.entity.*;
import com.upn.gestion_clinica.repository.CitaMedicaRepository;
import com.upn.gestion_clinica.repository.HorarioMedicoRepository;
import com.upn.gestion_clinica.repository.MedicoRepository;
import com.upn.gestion_clinica.repository.PacienteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitaMedicaServiceImplTest {

    @Mock
    private CitaMedicaRepository citaMedicaRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private MedicoRepository medicoRepository;

    @Mock
    private HorarioMedicoRepository horarioMedicoRepository;

    @InjectMocks
    private CitaMedicaServiceImpl citaMedicaService;

    private Paciente paciente;
    private Medico medico;
    private HorarioMedico horario;
    private LocalDate fechaValida;
    private LocalTime horaValida;

    @BeforeEach
    void setUp() {
        // Mock de Paciente
        paciente = new Paciente();
        paciente.setId(1);
        paciente.setNombre("Carlos");
        paciente.setApellido("Mendoza");
        paciente.setDocumentoIdentidad("72819283");

        // Mock de Usuario para Médico
        Usuario usuarioMedico = new Usuario();
        usuarioMedico.setNombre("Roberto");
        usuarioMedico.setApellido("Sanchez");

        // Mock de Especialidad
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre("Medicina General");

        // Mock de Médico
        medico = new Medico();
        medico.setId(10);
        medico.setUsuario(usuarioMedico);
        medico.setEspecialidad(especialidad);

        // Fecha y Horario: Supongamos una fecha fija (ej. Miércoles = día 3)
        fechaValida = LocalDate.of(2026, 10, 14); // Miércoles
        horaValida = LocalTime.of(10, 0); // 10:00 AM

        horario = new HorarioMedico();
        horario.setHoraInicio(LocalTime.of(8, 0));
        horario.setHoraFin(LocalTime.of(14, 0));
    }

    @Test
    @DisplayName("Camino Feliz: Debe agendar cita exitosamente con datos válidos y disponibilidad")
    void agendarCita_CaminoFeliz_DebeRegistrarExitosamente() {
        // Arrange
        CitaMedicaRequestDto request = new CitaMedicaRequestDto();
        request.setPacienteId(1);
        request.setMedicoId(10);
        request.setFechaCita(fechaValida);
        request.setHoraCita(horaValida);

        when(pacienteRepository.findById(1)).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(10)).thenReturn(Optional.of(medico));
        when(horarioMedicoRepository.findByMedicoIdAndDiaSemana(eq(10), anyInt()))
                .thenReturn(List.of(horario));
        when(citaMedicaRepository.existsByMedicoIdAndFechaCitaAndHoraCita(10, fechaValida, horaValida))
                .thenReturn(false);
        when(citaMedicaRepository.existsByPacienteIdAndFechaCitaAndHoraCita(1, fechaValida, horaValida))
                .thenReturn(false);

        when(citaMedicaRepository.save(any(CitaMedica.class))).thenAnswer(invocation -> {
            CitaMedica c = invocation.getArgument(0);
            c.setId(100);
            return c;
        });

        // Act
        CitaMedicaResponseDto response = citaMedicaService.agendarCita(request);

        // Assert
        assertNotNull(response);
        assertEquals(100, response.getId());
        assertEquals(1, response.getPacienteId());
        assertEquals(10, response.getMedicoId());
        assertEquals("Confirmada", response.getEstadoCita());
        verify(citaMedicaRepository, times(1)).save(any(CitaMedica.class));
    }

    @Test
    @DisplayName("Caso Borde: Debe permitir agendar exactamente a la hora de inicio del turno (08:00)")
    void agendarCita_CasoBorde_HoraExactaInicioTurno_DebeRegistrar() {
        // Arrange
        LocalTime horaAperturaExacta = LocalTime.of(8, 0); // Inicio exacto del turno
        CitaMedicaRequestDto request = new CitaMedicaRequestDto();
        request.setPacienteId(1);
        request.setMedicoId(10);
        request.setFechaCita(fechaValida);
        request.setHoraCita(horaAperturaExacta);

        when(pacienteRepository.findById(1)).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(10)).thenReturn(Optional.of(medico));
        when(horarioMedicoRepository.findByMedicoIdAndDiaSemana(eq(10), anyInt()))
                .thenReturn(List.of(horario));
        when(citaMedicaRepository.existsByMedicoIdAndFechaCitaAndHoraCita(10, fechaValida, horaAperturaExacta))
                .thenReturn(false);
        when(citaMedicaRepository.existsByPacienteIdAndFechaCitaAndHoraCita(1, fechaValida, horaAperturaExacta))
                .thenReturn(false);

        when(citaMedicaRepository.save(any(CitaMedica.class))).thenAnswer(invocation -> {
            CitaMedica c = invocation.getArgument(0);
            c.setId(101);
            return c;
        });

        // Act
        CitaMedicaResponseDto response = citaMedicaService.agendarCita(request);

        // Assert
        assertNotNull(response);
        assertEquals(horaAperturaExacta, response.getHoraCita());
        verify(citaMedicaRepository, times(1)).save(any(CitaMedica.class));
    }

    @Test
    @DisplayName("Caso Excepción: Debe lanzar excepción si el médico ya tiene otra cita en ese horario")
    void agendarCita_Excepcion_MedicoConCitaOcupada_LanzaIllegalArgumentException() {
        // Arrange
        CitaMedicaRequestDto request = new CitaMedicaRequestDto();
        request.setPacienteId(1);
        request.setMedicoId(10);
        request.setFechaCita(fechaValida);
        request.setHoraCita(horaValida);

        when(pacienteRepository.findById(1)).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(10)).thenReturn(Optional.of(medico));
        when(horarioMedicoRepository.findByMedicoIdAndDiaSemana(eq(10), anyInt()))
                .thenReturn(List.of(horario));
        
        // Simular que el médico ya tiene cita en ese horario
        when(citaMedicaRepository.existsByMedicoIdAndFechaCitaAndHoraCita(10, fechaValida, horaValida))
                .thenReturn(true);

        // Act & Assert
        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class, () -> {
            citaMedicaService.agendarCita(request);
        });

        assertEquals("El médico ya tiene una cita asignada en ese horario exacto.", excepcion.getMessage());
        verify(citaMedicaRepository, never()).save(any(CitaMedica.class));
    }

    @Test
    @DisplayName("Caso Excepción: Debe lanzar excepción si la cita está fuera del rango del médico")
    void agendarCita_Excepcion_FueraDeHorarioMedico_LanzaIllegalArgumentException() {
        // Arrange (15:00 está fuera de 08:00 - 14:00)
        LocalTime horaFueraDeTurno = LocalTime.of(15, 0);
        CitaMedicaRequestDto request = new CitaMedicaRequestDto();
        request.setPacienteId(1);
        request.setMedicoId(10);
        request.setFechaCita(fechaValida);
        request.setHoraCita(horaFueraDeTurno);

        when(pacienteRepository.findById(1)).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(10)).thenReturn(Optional.of(medico));
        when(horarioMedicoRepository.findByMedicoIdAndDiaSemana(eq(10), anyInt()))
                .thenReturn(List.of(horario));

        // Act & Assert
        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class, () -> {
            citaMedicaService.agendarCita(request);
        });

        assertTrue(excepcion.getMessage().contains("La cita debe programarse dentro de un horario disponible"));
        verify(citaMedicaRepository, never()).save(any(CitaMedica.class));
    }

    @Test
    @DisplayName("Caso Excepción: Debe lanzar excepción si el paciente no existe en el sistema")
    void agendarCita_Excepcion_PacienteNoExiste_LanzaEntityNotFoundException() {
        // Arrange
        CitaMedicaRequestDto request = new CitaMedicaRequestDto();
        request.setPacienteId(999);
        request.setMedicoId(10);

        when(pacienteRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            citaMedicaService.agendarCita(request);
        });

        verify(citaMedicaRepository, never()).save(any(CitaMedica.class));
    }
}
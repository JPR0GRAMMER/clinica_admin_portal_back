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
public class CitaMedicaServiceImplTest {

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
        paciente = new Paciente();
        paciente.setId(1);
        paciente.setNombre("Carlos");
        paciente.setApellido("Mendoza");
        paciente.setDocumentoIdentidad("72819283");

        Usuario usuarioMedico = new Usuario();
        usuarioMedico.setNombre("Roberto");
        usuarioMedico.setApellido("Sanchez");

        Especialidad especialidad = new Especialidad();
        especialidad.setNombre("Medicina General");

        medico = new Medico();
        medico.setId(10);
        medico.setUsuario(usuarioMedico);
        medico.setEspecialidad(especialidad);

        fechaValida = LocalDate.of(2026, 10, 14); // Miércoles
        horaValida = LocalTime.of(10, 0); // 10:00 AM

        horario = new HorarioMedico();
        horario.setHoraInicio(LocalTime.of(8, 0));
        horario.setHoraFin(LocalTime.of(14, 0));
    }

    // --- TEST 1 (Camino feliz) ---
    @Test
    @DisplayName("1. Camino Feliz: Registrar cita con datos válidos y disponibilidad")
    void agendarCita_CaminoFeliz_DebeRegistrarExitosamente() {
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

        CitaMedicaResponseDto response = citaMedicaService.agendarCita(request);

        assertNotNull(response);
        assertEquals(100, response.getId());
        assertEquals(1, response.getPacienteId());
        assertEquals(10, response.getMedicoId());
        assertEquals("Confirmada", response.getEstadoCita());
        verify(citaMedicaRepository, times(1)).save(any(CitaMedica.class));
    }

    // --- TEST 2 (Caso borde inferior) ---
    @Test
    @DisplayName("2. Caso Borde: Permitir agendar en el inicio exacto del turno (08:00)")
    void agendarCita_CasoBorde_HoraExactaInicioTurno_DebeRegistrar() {
        LocalTime horaAperturaExacta = LocalTime.of(8, 0);
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

        CitaMedicaResponseDto response = citaMedicaService.agendarCita(request);

        assertNotNull(response);
        assertEquals(horaAperturaExacta, response.getHoraCita());
        verify(citaMedicaRepository, times(1)).save(any(CitaMedica.class));
    }

    // --- TEST 3 (Caso borde superior) ---
    @Test
    @DisplayName("3. Caso Borde: Rechazar reserva en la hora exacta de fin del turno (14:00)")
    void agendarCita_CasoBorde_HoraExactaFinTurno_LanzaIllegalArgumentException() {
        LocalTime horaCierreExacta = LocalTime.of(14, 0);
        CitaMedicaRequestDto request = new CitaMedicaRequestDto();
        request.setPacienteId(1);
        request.setMedicoId(10);
        request.setFechaCita(fechaValida);
        request.setHoraCita(horaCierreExacta);

        when(pacienteRepository.findById(1)).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(10)).thenReturn(Optional.of(medico));
        when(horarioMedicoRepository.findByMedicoIdAndDiaSemana(eq(10), anyInt()))
                .thenReturn(List.of(horario));

        assertThrows(IllegalArgumentException.class, () -> citaMedicaService.agendarCita(request));
        verify(citaMedicaRepository, never()).save(any(CitaMedica.class));
    }

    // --- TEST 4 (Excepción solapamiento médico) ---
    @Test
    @DisplayName("4. Excepción: Lanzar error si el médico ya tiene otra cita en ese horario")
    void agendarCita_Excepcion_MedicoConCitaOcupada_LanzaIllegalArgumentException() {
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
                .thenReturn(true);

        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class, () -> {
            citaMedicaService.agendarCita(request);
        });

        assertEquals("El médico ya tiene una cita asignada en ese horario exacto.", excepcion.getMessage());
        verify(citaMedicaRepository, never()).save(any(CitaMedica.class));
    }

    // --- TEST 5 (Excepción solapamiento paciente) ---
    @Test
    @DisplayName("5. Excepción: Lanzar error si el paciente ya tiene otra cita en ese horario")
    void agendarCita_Excepcion_PacienteConCitaOcupada_LanzaIllegalArgumentException() {
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
                .thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            citaMedicaService.agendarCita(request);
        });

        assertEquals("El paciente ya tiene una cita asignada en ese horario exacto.", ex.getMessage());
        verify(citaMedicaRepository, never()).save(any(CitaMedica.class));
    }

    // --- TEST 6 (Excepción fuera de horario) ---
    @Test
    @DisplayName("6. Excepción: Lanzar error si la hora de la cita está fuera del turno del médico")
    void agendarCita_Excepcion_FueraDeHorarioMedico_LanzaIllegalArgumentException() {
        LocalTime horaFueraDeTurno = LocalTime.of(15, 0); // El turno acaba a las 14:00
        CitaMedicaRequestDto request = new CitaMedicaRequestDto();
        request.setPacienteId(1);
        request.setMedicoId(10);
        request.setFechaCita(fechaValida);
        request.setHoraCita(horaFueraDeTurno);

        when(pacienteRepository.findById(1)).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(10)).thenReturn(Optional.of(medico));
        when(horarioMedicoRepository.findByMedicoIdAndDiaSemana(eq(10), anyInt()))
                .thenReturn(List.of(horario));

        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class, () -> {
            citaMedicaService.agendarCita(request);
        });

        assertTrue(excepcion.getMessage().contains("La cita debe programarse dentro de un horario disponible"));
        verify(citaMedicaRepository, never()).save(any(CitaMedica.class));
    }

    // --- TEST 7 (Excepción sin horarios ese día) ---
    @Test
    @DisplayName("7. Excepción: Lanzar error si el médico no tiene turnos configurados para el día")
    void agendarCita_Excepcion_MedicoSinHorarioElDia_LanzaIllegalArgumentException() {
        CitaMedicaRequestDto request = new CitaMedicaRequestDto();
        request.setPacienteId(1);
        request.setMedicoId(10);
        request.setFechaCita(fechaValida);
        request.setHoraCita(horaValida);

        when(pacienteRepository.findById(1)).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(10)).thenReturn(Optional.of(medico));
        when(horarioMedicoRepository.findByMedicoIdAndDiaSemana(eq(10), anyInt()))
                .thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> citaMedicaService.agendarCita(request));
        verify(citaMedicaRepository, never()).save(any(CitaMedica.class));
    }

    // --- TEST 8 (Excepción paciente no existe) ---
    @Test
    @DisplayName("8. Excepción: Lanzar error si el paciente no existe en el sistema")
    void agendarCita_Excepcion_PacienteNoExiste_LanzaEntityNotFoundException() {
        CitaMedicaRequestDto request = new CitaMedicaRequestDto();
        request.setPacienteId(999);
        request.setMedicoId(10);

        when(pacienteRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> citaMedicaService.agendarCita(request));
        verify(citaMedicaRepository, never()).save(any(CitaMedica.class));
    }

    // --- TEST 9 (Excepción médico no existe) ---
    @Test
    @DisplayName("9. Excepción: Lanzar error si el médico no existe en el sistema")
    void agendarCita_Excepcion_MedicoNoExiste_LanzaEntityNotFoundException() {
        CitaMedicaRequestDto request = new CitaMedicaRequestDto();
        request.setPacienteId(1);
        request.setMedicoId(999);

        when(pacienteRepository.findById(1)).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> citaMedicaService.agendarCita(request));
        verify(citaMedicaRepository, never()).save(any(CitaMedica.class));
    }

    @Test
    @DisplayName("CitaMedicaServiceImpl.listarCitas() - filtra por médico autenticado")
    void listarCitas_FiltraPorMedico() {
        when(medicoRepository.findByUsuarioCorreo("medico@clinica.com")).thenReturn(Optional.of(medico));
        CitaMedica cita = cita(200, fechaValida, LocalTime.of(11, 0), EstadoCitaEnum.Confirmada);
        when(citaMedicaRepository.findAllByMedicoIdOrderByFechaCitaDescHoraCitaDesc(10))
                .thenReturn(List.of(cita));

        List<CitaMedicaResponseDto> response = citaMedicaService.listarCitas("medico@clinica.com", true);

        assertEquals(1, response.size());
        assertEquals(200, response.get(0).getId());
        verify(citaMedicaRepository).findAllByMedicoIdOrderByFechaCitaDescHoraCitaDesc(10);
        verify(citaMedicaRepository, never()).findAllByOrderByFechaCitaDescHoraCitaDesc();
    }

    @Test
    @DisplayName("CitaMedicaServiceImpl.listarCitas() - lista todas para recepción en orden descendente")
    void listarCitas_ListaTodasParaRecepcion() {
        CitaMedica masReciente = cita(202, fechaValida.plusDays(1), LocalTime.of(11, 0), EstadoCitaEnum.Confirmada);
        CitaMedica anterior = cita(201, fechaValida, LocalTime.of(9, 0), EstadoCitaEnum.Confirmada);
        when(citaMedicaRepository.findAllByOrderByFechaCitaDescHoraCitaDesc())
                .thenReturn(List.of(masReciente, anterior));

        List<CitaMedicaResponseDto> response = citaMedicaService.listarCitas("recepcion@clinica.com", false);

        assertEquals(List.of(202, 201), response.stream().map(CitaMedicaResponseDto::getId).toList());
        verify(citaMedicaRepository).findAllByOrderByFechaCitaDescHoraCitaDesc();
        verifyNoInteractions(medicoRepository);
    }

    @Test
    @DisplayName("CitaMedicaServiceImpl.listarCitas() - médico asociado inexistente")
    void listarCitas_RechazaMedicoInexistente() {
        when(medicoRepository.findByUsuarioCorreo("sin-medico@clinica.com")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> citaMedicaService.listarCitas("sin-medico@clinica.com", true));
        verify(citaMedicaRepository, never()).findAllByMedicoIdOrderByFechaCitaDescHoraCitaDesc(anyInt());
    }

    @Test
    @DisplayName("CitaMedicaServiceImpl.obtenerCitaPorId() - consulta válida para recepción")
    void obtenerCitaPorId_ConsultaValidaParaRecepcion() {
        CitaMedica cita = cita(300, fechaValida, horaValida, EstadoCitaEnum.Confirmada);
        when(citaMedicaRepository.findById(300)).thenReturn(Optional.of(cita));

        CitaMedicaResponseDto response = citaMedicaService.obtenerCitaPorId(300, "recepcion@clinica.com", false);

        assertEquals(300, response.getId());
        assertEquals("Carlos Mendoza", response.getPacienteNombreCompleto());
        assertEquals("Dr/Dra. Roberto Sanchez", response.getMedicoNombreCompleto());
    }

    @Test
    @DisplayName("CitaMedicaServiceImpl.obtenerCitaPorId() - cita inexistente")
    void obtenerCitaPorId_RechazaCitaInexistente() {
        when(citaMedicaRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> citaMedicaService.obtenerCitaPorId(999, "recepcion@clinica.com", false));
    }

    @Test
    @DisplayName("CitaMedicaServiceImpl.obtenerCitaPorId() - permite al médico autorizado")
    void obtenerCitaPorId_PermiteMedicoAutorizado() {
        CitaMedica cita = cita(301, fechaValida, horaValida, EstadoCitaEnum.Confirmada);
        when(citaMedicaRepository.findById(301)).thenReturn(Optional.of(cita));
        when(medicoRepository.findByUsuarioCorreo("medico@clinica.com")).thenReturn(Optional.of(medico));

        CitaMedicaResponseDto response = citaMedicaService.obtenerCitaPorId(301, "medico@clinica.com", true);

        assertEquals(301, response.getId());
        verify(medicoRepository).findByUsuarioCorreo("medico@clinica.com");
    }

    @Test
    @DisplayName("CitaMedicaServiceImpl.obtenerCitaPorId() - rechaza al médico que consulta una cita ajena")
    void obtenerCitaPorId_RechazaMedicoNoAutorizado() {
        CitaMedica cita = cita(302, fechaValida, horaValida, EstadoCitaEnum.Confirmada);
        Medico otroMedico = new Medico();
        otroMedico.setId(99);
        when(citaMedicaRepository.findById(302)).thenReturn(Optional.of(cita));
        when(medicoRepository.findByUsuarioCorreo("otro@clinica.com")).thenReturn(Optional.of(otroMedico));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> citaMedicaService.obtenerCitaPorId(302, "otro@clinica.com", true));

        assertEquals("No tienes permiso para ver esta cita", exception.getMessage());
    }

    @Test
    @DisplayName("CitaMedicaServiceImpl.reprogramarCita() - cambia fecha, médico y paciente")
    void reprogramarCita_CambiaFechaMedicoYPaciente() {
        CitaMedica cita = cita(400, fechaValida, horaValida, EstadoCitaEnum.Confirmada);
        Medico nuevoMedico = new Medico();
        nuevoMedico.setId(11);
        nuevoMedico.setUsuario(medico.getUsuario());
        nuevoMedico.setEspecialidad(medico.getEspecialidad());
        Paciente nuevoPaciente = new Paciente();
        nuevoPaciente.setId(2);
        nuevoPaciente.setNombre("Julia");
        nuevoPaciente.setApellido("Ramos");
        nuevoPaciente.setDocumentoIdentidad("71234567");
        CitaMedicaRequestDto request = request(2, 11, fechaValida.plusDays(2), LocalTime.of(12, 0));

        when(citaMedicaRepository.findById(400)).thenReturn(Optional.of(cita));
        when(horarioMedicoRepository.findByMedicoIdAndDiaSemana(eq(11), anyInt())).thenReturn(List.of(horario));
        when(citaMedicaRepository.existsByMedicoIdAndFechaCitaAndHoraCita(11, request.getFechaCita(), request.getHoraCita()))
                .thenReturn(false);
        when(citaMedicaRepository.existsByPacienteIdAndFechaCitaAndHoraCita(2, request.getFechaCita(), request.getHoraCita()))
                .thenReturn(false);
        when(medicoRepository.findById(11)).thenReturn(Optional.of(nuevoMedico));
        when(pacienteRepository.findById(2)).thenReturn(Optional.of(nuevoPaciente));
        when(citaMedicaRepository.save(any(CitaMedica.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CitaMedicaResponseDto response = citaMedicaService.reprogramarCita(400, request);

        assertEquals(11, response.getMedicoId());
        assertEquals(2, response.getPacienteId());
        assertEquals(fechaValida.plusDays(2), response.getFechaCita());
        assertEquals("Reprogramada", response.getEstadoCita());
        verify(citaMedicaRepository).save(cita);
    }

    @Test
    @DisplayName("CitaMedicaServiceImpl.reprogramarCita() - rechaza conflicto de disponibilidad")
    void reprogramarCita_RechazaConflictoDeDisponibilidad() {
        CitaMedica cita = cita(401, fechaValida, horaValida, EstadoCitaEnum.Confirmada);
        CitaMedicaRequestDto request = request(1, 10, fechaValida.plusDays(1), horaValida);
        when(citaMedicaRepository.findById(401)).thenReturn(Optional.of(cita));
        when(horarioMedicoRepository.findByMedicoIdAndDiaSemana(eq(10), anyInt())).thenReturn(List.of(horario));
        when(citaMedicaRepository.existsByMedicoIdAndFechaCitaAndHoraCita(10, request.getFechaCita(), request.getHoraCita()))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> citaMedicaService.reprogramarCita(401, request));
        verify(citaMedicaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CitaMedicaServiceImpl.reprogramarCita() - preserva estados Asistió y Cancelada")
    void reprogramarCita_PreservaEstadosFinales() {
        CitaMedicaRequestDto request = request(1, 10, fechaValida, horaValida);
        when(horarioMedicoRepository.findByMedicoIdAndDiaSemana(eq(10), anyInt())).thenReturn(List.of(horario));
        when(citaMedicaRepository.save(any(CitaMedica.class))).thenAnswer(invocation -> invocation.getArgument(0));

        for (EstadoCitaEnum estado : List.of(EstadoCitaEnum.Asistió, EstadoCitaEnum.Cancelada)) {
            CitaMedica cita = cita(410, fechaValida, horaValida, estado);
            when(citaMedicaRepository.findById(410)).thenReturn(Optional.of(cita));

            CitaMedicaResponseDto response = citaMedicaService.reprogramarCita(410, request);

            assertEquals(estado.name().replace("_", " "), response.getEstadoCita());
        }
    }

    @Test
    @DisplayName("CitaMedicaServiceImpl.cambiarEstadoCita() - guarda cada estado del enum")
    void cambiarEstadoCita_GuardaCadaEstado() {
        CitaMedica cita = cita(500, fechaValida, horaValida, EstadoCitaEnum.Confirmada);
        when(citaMedicaRepository.findById(500)).thenReturn(Optional.of(cita));

        for (EstadoCitaEnum estado : EstadoCitaEnum.values()) {
            citaMedicaService.cambiarEstadoCita(500, estado);
            assertEquals(estado, cita.getEstadoCita());
        }

        verify(citaMedicaRepository, times(EstadoCitaEnum.values().length)).save(cita);
    }

    @Test
    @DisplayName("CitaMedicaServiceImpl.cambiarEstadoCita() - cita inexistente")
    void cambiarEstadoCita_RechazaCitaInexistente() {
        when(citaMedicaRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> citaMedicaService.cambiarEstadoCita(999, EstadoCitaEnum.Cancelada));
        verify(citaMedicaRepository, never()).save(any());
    }

    private CitaMedicaRequestDto request(Integer pacienteId, Integer medicoId, LocalDate fecha, LocalTime hora) {
        CitaMedicaRequestDto request = new CitaMedicaRequestDto();
        request.setPacienteId(pacienteId);
        request.setMedicoId(medicoId);
        request.setFechaCita(fecha);
        request.setHoraCita(hora);
        return request;
    }

    private CitaMedica cita(Integer id, LocalDate fecha, LocalTime hora, EstadoCitaEnum estado) {
        CitaMedica cita = new CitaMedica();
        cita.setId(id);
        cita.setPaciente(paciente);
        cita.setMedico(medico);
        cita.setFechaCita(fecha);
        cita.setHoraCita(hora);
        cita.setEstadoCita(estado);
        return cita;
    }
}

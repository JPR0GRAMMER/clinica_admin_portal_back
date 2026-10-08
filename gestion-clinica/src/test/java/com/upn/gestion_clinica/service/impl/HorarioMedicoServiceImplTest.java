package com.upn.gestion_clinica.service.impl;

import com.upn.gestion_clinica.dto.horario_medico.HorarioMedicoRequestDto;
import com.upn.gestion_clinica.dto.horario_medico.HorarioMedicoResponseDto;
import com.upn.gestion_clinica.entity.CitaMedica;
import com.upn.gestion_clinica.entity.EstadoCitaEnum;
import com.upn.gestion_clinica.entity.Especialidad;
import com.upn.gestion_clinica.entity.HorarioMedico;
import com.upn.gestion_clinica.entity.Medico;
import com.upn.gestion_clinica.entity.Paciente;
import com.upn.gestion_clinica.entity.Usuario;
import com.upn.gestion_clinica.repository.CitaMedicaRepository;
import com.upn.gestion_clinica.repository.HorarioMedicoRepository;
import com.upn.gestion_clinica.repository.MedicoRepository;
import jakarta.persistence.EntityNotFoundException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HorarioMedicoServiceImplTest {

    @Mock
    private HorarioMedicoRepository horarioMedicoRepository;

    @Mock
    private MedicoRepository medicoRepository;

    @Mock
    private CitaMedicaRepository citaMedicaRepository;

    @InjectMocks
    private HorarioMedicoServiceImpl horarioService;

    private final Medico medico = medico(10, "Roberto", "Sanchez");

    @Test
    @DisplayName("HorarioMedicoServiceImpl.crear() - crea horario válido")
    void crear_CreaHorarioValido() {
        HorarioMedicoRequestDto request = request(10, 3, "08:00", "14:00");
        HorarioMedico horarioGuardado = horario(50, medico, 3, "08:00", "14:00");
        when(horarioMedicoRepository.findByMedicoIdAndDiaSemana(10, 3)).thenReturn(List.of());
        when(medicoRepository.findById(10)).thenReturn(Optional.of(medico));
        when(horarioMedicoRepository.save(any(HorarioMedico.class))).thenReturn(horarioGuardado);

        HorarioMedicoResponseDto response = horarioService.crear(request);

        assertEquals(50, response.getId());
        assertEquals(10, response.getMedicoId());
        assertEquals(3, response.getDiaSemana());
        assertEquals(LocalTime.of(8, 0), response.getHoraInicio());
        assertEquals(LocalTime.of(14, 0), response.getHoraFin());
        verify(horarioMedicoRepository).save(any(HorarioMedico.class));
    }

    @Test
    @DisplayName("HorarioMedicoServiceImpl.crear() - rechaza hora final igual o menor")
    void crear_RechazaHoraFinalInvalida() {
        HorarioMedicoRequestDto igual = request(10, 3, "08:00", "08:00");
        HorarioMedicoRequestDto menor = request(10, 3, "10:00", "08:00");

        assertThrows(IllegalArgumentException.class, () -> horarioService.crear(igual));
        assertThrows(IllegalArgumentException.class, () -> horarioService.crear(menor));
        verifyNoInteractions(horarioMedicoRepository, medicoRepository);
    }

    @Test
    @DisplayName("HorarioMedicoServiceImpl.crear() - rechaza solapamiento parcial")
    void crear_RechazaSolapamientoParcial() {
        HorarioMedicoRequestDto request = request(10, 3, "13:00", "16:00");
        when(horarioMedicoRepository.findByMedicoIdAndDiaSemana(10, 3))
                .thenReturn(List.of(horario(1, medico, 3, "08:00", "14:00")));

        assertThrows(IllegalArgumentException.class, () -> horarioService.crear(request));
        verify(medicoRepository, never()).findById(anyInt());
    }

    @Test
    @DisplayName("HorarioMedicoServiceImpl.crear() - rechaza solapamiento total")
    void crear_RechazaSolapamientoTotal() {
        HorarioMedicoRequestDto request = request(10, 3, "09:00", "12:00");
        when(horarioMedicoRepository.findByMedicoIdAndDiaSemana(10, 3))
                .thenReturn(List.of(horario(1, medico, 3, "08:00", "14:00")));

        assertThrows(IllegalArgumentException.class, () -> horarioService.crear(request));
        verify(horarioMedicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("HorarioMedicoServiceImpl.crear() - permite límites contiguos")
    void crear_PermiteLimitesContiguos() {
        HorarioMedicoRequestDto request = request(10, 3, "14:00", "16:00");
        when(horarioMedicoRepository.findByMedicoIdAndDiaSemana(10, 3))
                .thenReturn(List.of(horario(1, medico, 3, "08:00", "14:00")));
        when(medicoRepository.findById(10)).thenReturn(Optional.of(medico));
        when(horarioMedicoRepository.save(any(HorarioMedico.class))).thenAnswer(invocation -> {
            HorarioMedico horario = invocation.getArgument(0);
            horario.setId(2);
            return horario;
        });

        HorarioMedicoResponseDto response = horarioService.crear(request);

        assertEquals(2, response.getId());
        assertEquals(LocalTime.of(14, 0), response.getHoraInicio());
    }

    @Test
    @DisplayName("HorarioMedicoServiceImpl.crear() - médico inexistente")
    void crear_RechazaMedicoInexistente() {
        HorarioMedicoRequestDto request = request(999, 3, "08:00", "14:00");
        when(horarioMedicoRepository.findByMedicoIdAndDiaSemana(999, 3)).thenReturn(List.of());
        when(medicoRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> horarioService.crear(request));
        verify(horarioMedicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("HorarioMedicoServiceImpl.actualizar() - actualiza excluyendo su propio horario")
    void actualizar_ActualizaExcluyendoPropioHorario() {
        HorarioMedico actual = horario(5, medico, 3, "08:00", "12:00");
        HorarioMedicoRequestDto request = request(10, 3, "09:00", "13:00");
        when(horarioMedicoRepository.findById(5)).thenReturn(Optional.of(actual));
        when(horarioMedicoRepository.findByMedicoIdAndDiaSemana(10, 3)).thenReturn(List.of(actual));
        when(medicoRepository.findById(10)).thenReturn(Optional.of(medico));
        when(horarioMedicoRepository.findByMedicoId(10)).thenReturn(List.of(actual));
        when(citaMedicaRepository.findByMedicoIdAndFechaCitaGreaterThanEqual(eq(10), any(LocalDate.class)))
                .thenReturn(List.of());
        when(horarioMedicoRepository.save(any(HorarioMedico.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HorarioMedicoResponseDto response = horarioService.actualizar(5, request);

        assertEquals(5, response.getId());
        assertEquals(LocalTime.of(9, 0), response.getHoraInicio());
        assertEquals(LocalTime.of(13, 0), response.getHoraFin());
        verify(horarioMedicoRepository).save(actual);
    }

    @Test
    @DisplayName("HorarioMedicoServiceImpl.actualizar() - rechaza solapamiento")
    void actualizar_RechazaSolapamiento() {
        HorarioMedico actual = horario(5, medico, 3, "08:00", "12:00");
        HorarioMedico otro = horario(6, medico, 3, "14:00", "18:00");
        when(horarioMedicoRepository.findById(5)).thenReturn(Optional.of(actual));
        when(horarioMedicoRepository.findByMedicoIdAndDiaSemana(10, 3)).thenReturn(List.of(actual, otro));

        assertThrows(IllegalArgumentException.class,
                () -> horarioService.actualizar(5, request(10, 3, "17:00", "19:00")));
        verify(horarioMedicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("HorarioMedicoServiceImpl.actualizar() - impide dejar citas futuras sin cobertura")
    void actualizar_RechazaCitasFuturasSinCobertura() {
        HorarioMedico actual = horario(5, medico, 3, "08:00", "12:00");
        HorarioMedicoRequestDto request = request(10, 3, 3, "08:00", "09:00");
        CitaMedica citaFutura = citaFutura(medico, LocalDate.now().plusDays(1), LocalTime.of(10, 0));
        when(horarioMedicoRepository.findById(5)).thenReturn(Optional.of(actual));
        when(horarioMedicoRepository.findByMedicoIdAndDiaSemana(10, 3)).thenReturn(List.of(actual));
        when(medicoRepository.findById(10)).thenReturn(Optional.of(medico));
        when(horarioMedicoRepository.findByMedicoId(10)).thenReturn(List.of(actual));
        when(citaMedicaRepository.findByMedicoIdAndFechaCitaGreaterThanEqual(eq(10), any(LocalDate.class)))
                .thenReturn(List.of(citaFutura));

        assertThrows(IllegalArgumentException.class, () -> horarioService.actualizar(5, request));
        verify(horarioMedicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("HorarioMedicoServiceImpl.actualizar() - permite cambio de médico sin citas descubiertas")
    void actualizar_PermiteCambioDeMedico() {
        Medico nuevoMedico = medico(11, "Julia", "Ramos");
        HorarioMedico actual = horario(5, medico, 3, "08:00", "12:00");
        HorarioMedicoRequestDto request = request(11, 3, "09:00", "13:00");
        when(horarioMedicoRepository.findById(5)).thenReturn(Optional.of(actual));
        when(horarioMedicoRepository.findByMedicoIdAndDiaSemana(11, 3)).thenReturn(List.of());
        when(medicoRepository.findById(11)).thenReturn(Optional.of(nuevoMedico));
        when(horarioMedicoRepository.findByMedicoId(10)).thenReturn(List.of(actual));
        when(horarioMedicoRepository.findByMedicoId(11)).thenReturn(List.of());
        when(citaMedicaRepository.findByMedicoIdAndFechaCitaGreaterThanEqual(eq(10), any(LocalDate.class)))
                .thenReturn(List.of());
        when(citaMedicaRepository.findByMedicoIdAndFechaCitaGreaterThanEqual(eq(11), any(LocalDate.class)))
                .thenReturn(List.of());
        when(horarioMedicoRepository.save(any(HorarioMedico.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HorarioMedicoResponseDto response = horarioService.actualizar(5, request);

        assertEquals(11, response.getMedicoId());
        assertEquals(nuevoMedico, actual.getMedico());
    }

    @Test
    @DisplayName("HorarioMedicoServiceImpl.eliminar() - elimina horario cuando las citas futuras quedan cubiertas")
    void eliminar_EliminaConCitasFuturasCubiertas() {
        LocalDate fechaCita = LocalDate.now().plusDays(1);
        int diaCita = fechaCita.getDayOfWeek().getValue();
        HorarioMedico aEliminar = horario(5, medico, diaCita, "08:00", "12:00");
        HorarioMedico cobertura = horario(6, medico, diaCita, "12:00", "16:00");
        CitaMedica citaFutura = citaFutura(medico, fechaCita, LocalTime.of(13, 0));
        when(horarioMedicoRepository.findById(5)).thenReturn(Optional.of(aEliminar));
        when(horarioMedicoRepository.findByMedicoId(10)).thenReturn(List.of(aEliminar, cobertura));
        when(citaMedicaRepository.findByMedicoIdAndFechaCitaGreaterThanEqual(eq(10), any(LocalDate.class)))
                .thenReturn(List.of(citaFutura));

        horarioService.eliminar(5);

        verify(horarioMedicoRepository).deleteById(5);
    }

    @Test
    @DisplayName("HorarioMedicoServiceImpl.eliminar() - rechaza citas futuras fuera de disponibilidad")
    void eliminar_RechazaCitasFuturasSinCobertura() {
        HorarioMedico aEliminar = horario(5, medico, 3, "08:00", "12:00");
        CitaMedica citaFutura = citaFutura(medico, LocalDate.now().plusDays(1), LocalTime.of(10, 0));
        when(horarioMedicoRepository.findById(5)).thenReturn(Optional.of(aEliminar));
        when(horarioMedicoRepository.findByMedicoId(10)).thenReturn(List.of(aEliminar));
        when(citaMedicaRepository.findByMedicoIdAndFechaCitaGreaterThanEqual(eq(10), any(LocalDate.class)))
                .thenReturn(List.of(citaFutura));

        assertThrows(IllegalArgumentException.class, () -> horarioService.eliminar(5));
        verify(horarioMedicoRepository, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("HorarioMedicoServiceImpl.eliminar() - horario inexistente")
    void eliminar_RechazaHorarioInexistente() {
        when(horarioMedicoRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> horarioService.eliminar(999));
        verify(horarioMedicoRepository, never()).deleteById(anyInt());
    }

    private HorarioMedicoRequestDto request(Integer medicoId, Integer dia, String inicio, String fin) {
        return request(medicoId, dia, dia, inicio, fin);
    }

    private HorarioMedicoRequestDto request(Integer medicoId, Integer dia, Integer ignoredDia, String inicio, String fin) {
        HorarioMedicoRequestDto request = new HorarioMedicoRequestDto();
        request.setMedicoId(medicoId);
        request.setDiaSemana(dia);
        request.setHoraInicio(LocalTime.parse(inicio));
        request.setHoraFin(LocalTime.parse(fin));
        return request;
    }

    private HorarioMedico horario(Integer id, Medico medico, Integer dia, String inicio, String fin) {
        HorarioMedico horario = new HorarioMedico();
        horario.setId(id);
        horario.setMedico(medico);
        horario.setDiaSemana(dia);
        horario.setHoraInicio(LocalTime.parse(inicio));
        horario.setHoraFin(LocalTime.parse(fin));
        return horario;
    }

    private Medico medico(Integer id, String nombre, String apellido) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre("Medicina General");
        Medico medico = new Medico();
        medico.setId(id);
        medico.setUsuario(usuario);
        medico.setEspecialidad(especialidad);
        medico.setNumeroColegiatura("CMP-" + id);
        return medico;
    }

    private CitaMedica citaFutura(Medico medico, LocalDate fecha, LocalTime hora) {
        CitaMedica cita = new CitaMedica();
        cita.setMedico(medico);
        cita.setFechaCita(fecha);
        cita.setHoraCita(hora);
        cita.setEstadoCita(EstadoCitaEnum.Confirmada);
        return cita;
    }
}

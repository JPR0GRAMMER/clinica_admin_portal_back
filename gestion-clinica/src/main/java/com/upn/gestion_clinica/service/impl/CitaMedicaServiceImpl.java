package com.upn.gestion_clinica.service.impl;

import com.upn.gestion_clinica.dto.cita.CitaMedicaRequestDto;
import com.upn.gestion_clinica.dto.cita.CitaMedicaResponseDto;
import com.upn.gestion_clinica.entity.CitaMedica;
import com.upn.gestion_clinica.entity.EstadoCitaEnum;
import com.upn.gestion_clinica.entity.Medico;
import com.upn.gestion_clinica.entity.Paciente;
import com.upn.gestion_clinica.entity.HorarioMedico;
import com.upn.gestion_clinica.repository.CitaMedicaRepository;
import com.upn.gestion_clinica.repository.HorarioMedicoRepository;
import com.upn.gestion_clinica.repository.MedicoRepository;
import com.upn.gestion_clinica.repository.PacienteRepository;
import com.upn.gestion_clinica.service.CitaMedicaService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CitaMedicaServiceImpl implements CitaMedicaService {
    private static final int SIN_CITA_EXISTENTE = 0; // Los identificadores generados comienzan en 1.

    private final CitaMedicaRepository citaMedicaRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final HorarioMedicoRepository horarioMedicoRepository;

    public CitaMedicaServiceImpl(CitaMedicaRepository citaMedicaRepository, 
                                 PacienteRepository pacienteRepository, 
                                 MedicoRepository medicoRepository,
                                 HorarioMedicoRepository horarioMedicoRepository) {
        this.citaMedicaRepository = citaMedicaRepository;
        this.pacienteRepository = pacienteRepository;
        this.medicoRepository = medicoRepository;
        this.horarioMedicoRepository = horarioMedicoRepository;
    }

    @Override
    @Transactional
    public CitaMedicaResponseDto agendarCita(CitaMedicaRequestDto requestDto) {
        CitaMedica cita = new CitaMedica();
        asignarCita(cita, requestDto, SIN_CITA_EXISTENTE);
        cita.setEstadoCita(EstadoCitaEnum.Confirmada);
        return mapearADto(citaMedicaRepository.save(cita));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaMedicaResponseDto> listarCitas(String correoUsuario, boolean esMedico) {
        if (esMedico) {
            Medico medico = medicoRepository.findByUsuarioCorreo(correoUsuario)
                    .orElseThrow(() -> new EntityNotFoundException("No se encontró un médico asociado a tu usuario"));
            return citaMedicaRepository.findAllByMedicoIdOrderByFechaCitaDescHoraCitaDesc(medico.getId()).stream()
                    .map(this::mapearADto)
                    .collect(Collectors.toList());
        }
        

        return citaMedicaRepository.findAllByOrderByFechaCitaDescHoraCitaDesc().stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CitaMedicaResponseDto obtenerCitaPorId(Integer id, String correoUsuario, boolean esMedico) {
        CitaMedica cita = citaMedicaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cita médica no encontrada"));
                
        if (esMedico) {
            Medico medico = medicoRepository.findByUsuarioCorreo(correoUsuario)
                    .orElseThrow(() -> new EntityNotFoundException("No se encontró un médico asociado a tu usuario"));

            if (!cita.getMedico().getId().equals(medico.getId())) {
                throw new IllegalArgumentException("No tienes permiso para ver esta cita");
            }
        }
        
        return mapearADto(cita);
    }

    @Override
    @Transactional
    public CitaMedicaResponseDto reprogramarCita(Integer id, CitaMedicaRequestDto requestDto) {
        CitaMedica cita = citaMedicaRepository.buscarParaActualizar(id)
                .orElseThrow(() -> new EntityNotFoundException("Cita médica no encontrada"));

        if (!cita.getEstadoCita().permiteReprogramar()) {
            throw new IllegalArgumentException("Solo se pueden reprogramar citas confirmadas o reprogramadas.");
        }

        asignarCita(cita, requestDto, id);
        cita.setEstadoCita(EstadoCitaEnum.Reprogramada);
        return mapearADto(citaMedicaRepository.save(cita));
    }

    private void asignarCita(CitaMedica cita, CitaMedicaRequestDto requestDto, int idExcluido) {
        // Siempre se adquieren los bloqueos en el orden paciente, médico.
        Paciente paciente = bloquearPaciente(requestDto.getPacienteId());
        Medico medico = bloquearMedico(requestDto.getMedicoId());
        validarDisponibilidadMedico(requestDto.getMedicoId(), requestDto.getFechaCita(), requestDto.getHoraCita());
        validarConflictos(paciente, medico, requestDto.getFechaCita(), requestDto.getHoraCita(), idExcluido);

        cita.setPaciente(paciente);
        cita.setMedico(medico);
        cita.setFechaCita(requestDto.getFechaCita());
        cita.setHoraCita(requestDto.getHoraCita());
    }

    private void validarConflictos(Paciente paciente, Medico medico, LocalDate fecha, LocalTime hora, int idExcluido) {
        EstadoCitaEnum cancelada = EstadoCitaEnum.Cancelada;
        if (citaMedicaRepository.existsByPacienteIdAndMedicoEspecialidadIdAndFechaCitaAndEstadoCitaNotAndIdNot(
                paciente.getId(), medico.getEspecialidad().getId(), fecha, cancelada, idExcluido)) {
            throw new IllegalArgumentException("El paciente ya cuenta con una cita programada para esta misma especialidad en la fecha seleccionada.");
        }
        if (citaMedicaRepository.existsByMedicoIdAndFechaCitaAndHoraCitaAndEstadoCitaNotAndIdNot(
                medico.getId(), fecha, hora, cancelada, idExcluido)) {
            throw new IllegalArgumentException("El médico ya tiene una cita asignada en ese horario exacto.");
        }
        if (citaMedicaRepository.existsByPacienteIdAndFechaCitaAndHoraCitaAndEstadoCitaNotAndIdNot(
                paciente.getId(), fecha, hora, cancelada, idExcluido)) {
            throw new IllegalArgumentException("El paciente ya tiene una cita asignada en ese horario exacto.");
        }
    }

    private Paciente bloquearPaciente(Integer id) {
        return pacienteRepository.buscarParaActualizar(id)
                .orElseThrow(() -> new EntityNotFoundException("Paciente no encontrado"));
    }

    private Medico bloquearMedico(Integer id) {
        return medicoRepository.buscarParaActualizar(id)
                .orElseThrow(() -> new EntityNotFoundException("Médico no encontrado"));
    }

    @Override
    @Transactional
    public void cambiarEstadoCita(Integer id, EstadoCitaEnum nuevoEstado) {
        CitaMedica cita = citaMedicaRepository.buscarParaActualizar(id)
                .orElseThrow(() -> new EntityNotFoundException("Cita médica no encontrada"));
        if (cita.getEstadoCita() == EstadoCitaEnum.Cancelada && nuevoEstado != EstadoCitaEnum.Cancelada) {
            Paciente paciente = bloquearPaciente(cita.getPaciente().getId());
            Medico medico = bloquearMedico(cita.getMedico().getId());
            validarDisponibilidadMedico(medico.getId(), cita.getFechaCita(), cita.getHoraCita());
            validarConflictos(paciente, medico, cita.getFechaCita(), cita.getHoraCita(), id);
        }
        cita.setEstadoCita(nuevoEstado);
        citaMedicaRepository.save(cita);
    }

    private CitaMedicaResponseDto mapearADto(CitaMedica cita) {
        CitaMedicaResponseDto dto = new CitaMedicaResponseDto();
        dto.setId(cita.getId());
        dto.setPacienteId(cita.getPaciente().getId());
        dto.setPacienteNombreCompleto(cita.getPaciente().getNombre() + " " + cita.getPaciente().getApellido());
        dto.setPacienteDni(cita.getPaciente().getDocumentoIdentidad());
        
        dto.setMedicoId(cita.getMedico().getId());
        dto.setMedicoNombreCompleto("Dr/Dra. " + cita.getMedico().getUsuario().getNombre() + " " + cita.getMedico().getUsuario().getApellido());
        dto.setMedicoEspecialidad(cita.getMedico().getEspecialidad().getNombre());
        
        dto.setFechaCita(cita.getFechaCita());
        dto.setHoraCita(cita.getHoraCita());
        dto.setEstadoCita(cita.getEstadoCita().valorApi());
        return dto;
    }

    private void validarDisponibilidadMedico(Integer medicoId, LocalDate fecha, LocalTime hora) {
        int diaCita = fecha.getDayOfWeek().getValue();
        boolean dentroDeHorario = horarioMedicoRepository
                .findByMedicoIdAndDiaSemana(medicoId, diaCita)
                .stream()
                .anyMatch(horario -> !hora.isBefore(horario.getHoraInicio()) && hora.isBefore(horario.getHoraFin()));

        if (!dentroDeHorario) {
            throw new IllegalArgumentException(
                    "La cita debe programarse dentro de un horario disponible del médico seleccionado.");
        }
    }
}

package com.upn.gestion_clinica.service.impl;

import com.upn.gestion_clinica.dto.cita.CitaMedicaRequestDto;
import com.upn.gestion_clinica.dto.cita.CitaMedicaResponseDto;
import com.upn.gestion_clinica.entity.CitaMedica;
import com.upn.gestion_clinica.entity.EstadoCitaEnum;
import com.upn.gestion_clinica.entity.HorarioMedico;
import com.upn.gestion_clinica.entity.Medico;
import com.upn.gestion_clinica.entity.Paciente;
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

@Service
public class CitaMedicaServiceImpl implements CitaMedicaService {

    private static final int SIN_CITA_EXISTENTE = 0;
    private static final LocalTime HORA_APERTURA_CLINICA = LocalTime.of(8, 0);
    private static final String PREFIJO_MEDICO = "Dr/Dra. ";

    private static final String ERR_CITA_NO_ENCONTRADA = "Cita médica no encontrada";
    private static final String ERR_PACIENTE_NO_ENCONTRADO = "Paciente no encontrado";
    private static final String ERR_MEDICO_NO_ENCONTRADO = "Médico no encontrado";
    private static final String ERR_MEDICO_USUARIO_NO_ENCONTRADO = "No se encontró un médico asociado a tu usuario";
    private static final String ERR_SIN_PERMISO_CITA = "No tienes permiso para ver esta cita";
    private static final String ERR_ESTADO_NO_REPROGRAMABLE = "Solo se pueden reprogramar citas confirmadas o reprogramadas.";
    private static final String ERR_FUERA_DE_HORARIO = "La cita debe programarse dentro de un horario disponible del médico seleccionado.";
    private static final String ERR_CONFLICTO_ESPECIALIDAD_DIA = "El paciente ya cuenta con una cita programada para esta misma especialidad en la fecha seleccionada.";
    private static final String ERR_CONFLICTO_HORARIO_MEDICO = "El médico ya tiene una cita asignada en ese horario exacto.";
    private static final String ERR_CONFLICTO_HORARIO_PACIENTE = "El paciente ya tiene una cita asignada en ese horario exacto.";

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
        asignarYValidarCita(cita, requestDto, SIN_CITA_EXISTENTE);
        cita.setEstadoCita(EstadoCitaEnum.Confirmada);
        return mapearADto(citaMedicaRepository.save(cita));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaMedicaResponseDto> listarCitas(String correoUsuario, boolean esMedico) {
        List<CitaMedica> citas = esMedico
                ? citaMedicaRepository.findAllByMedicoIdOrderByFechaCitaDescHoraCitaDesc(obtenerMedicoPorCorreo(correoUsuario).getId())
                : citaMedicaRepository.findAllByOrderByFechaCitaDescHoraCitaDesc();

        return citas.stream()
                .map(this::mapearADto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CitaMedicaResponseDto obtenerCitaPorId(Integer id, String correoUsuario, boolean esMedico) {
        CitaMedica cita = citaMedicaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ERR_CITA_NO_ENCONTRADA));

        if (esMedico) {
            Medico medicoAutenticado = obtenerMedicoPorCorreo(correoUsuario);
            if (!cita.getMedico().getId().equals(medicoAutenticado.getId())) {
                throw new IllegalArgumentException(ERR_SIN_PERMISO_CITA);
            }
        }

        return mapearADto(cita);
    }

    @Override
    @Transactional
    public CitaMedicaResponseDto reprogramarCita(Integer id, CitaMedicaRequestDto requestDto) {
        CitaMedica cita = obtenerCitaParaActualizar(id);

        if (!cita.getEstadoCita().permiteReprogramar()) {
            throw new IllegalArgumentException(ERR_ESTADO_NO_REPROGRAMABLE);
        }

        asignarYValidarCita(cita, requestDto, id);
        cita.setEstadoCita(EstadoCitaEnum.Reprogramada);
        return mapearADto(citaMedicaRepository.save(cita));
    }

    @Override
    @Transactional
    public void cambiarEstadoCita(Integer id, EstadoCitaEnum nuevoEstado) {
        CitaMedica cita = obtenerCitaParaActualizar(id);

        if (esReactivacionDeCitaCancelada(cita.getEstadoCita(), nuevoEstado)) {
            Paciente paciente = bloquearPaciente(cita.getPaciente().getId());
            Medico medico = bloquearMedico(cita.getMedico().getId());
            validarDisponibilidadMedico(medico.getId(), cita.getFechaCita(), cita.getHoraCita());
            validarConflictos(paciente, medico, cita.getFechaCita(), cita.getHoraCita(), id);
        }

        cita.setEstadoCita(nuevoEstado);
        citaMedicaRepository.save(cita);
    }

    private void asignarYValidarCita(CitaMedica cita, CitaMedicaRequestDto requestDto, int idExcluido) {
        // Se adquieren los bloqueos siempre en el orden: paciente -> médico para evitar deadlocks.
        Paciente paciente = bloquearPaciente(requestDto.getPacienteId());
        Medico medico = bloquearMedico(requestDto.getMedicoId());

        validarDisponibilidadMedico(medico.getId(), requestDto.getFechaCita(), requestDto.getHoraCita());
        validarConflictos(paciente, medico, requestDto.getFechaCita(), requestDto.getHoraCita(), idExcluido);

        cita.setPaciente(paciente);
        cita.setMedico(medico);
        cita.setFechaCita(requestDto.getFechaCita());
        cita.setHoraCita(requestDto.getHoraCita());
    }

    private void validarDisponibilidadMedico(Integer medicoId, LocalDate fecha, LocalTime hora) {
        if (hora == null || hora.isBefore(HORA_APERTURA_CLINICA)) {
            throw new IllegalArgumentException(ERR_FUERA_DE_HORARIO);
        }

        int diaSemana = fecha.getDayOfWeek().getValue();
        boolean dentroDeHorario = horarioMedicoRepository
                .findByMedicoIdAndDiaSemana(medicoId, diaSemana)
                .stream()
                .anyMatch(horario -> estaDentroDeRangoHorario(hora, horario));

        if (!dentroDeHorario) {
            throw new IllegalArgumentException(ERR_FUERA_DE_HORARIO);
        }
    }

    private boolean estaDentroDeRangoHorario(LocalTime horaCita, HorarioMedico horario) {
        LocalTime horaInicioEfectiva = horario.getHoraInicio().isBefore(HORA_APERTURA_CLINICA)
                ? HORA_APERTURA_CLINICA
                : horario.getHoraInicio();

        return !horaCita.isBefore(horaInicioEfectiva) && horaCita.isBefore(horario.getHoraFin());
    }

    private void validarConflictos(Paciente paciente, Medico medico, LocalDate fecha, LocalTime hora, int idExcluido) {
        EstadoCitaEnum estadoCancelada = EstadoCitaEnum.Cancelada;

        if (citaMedicaRepository.existsByPacienteIdAndMedicoEspecialidadIdAndFechaCitaAndEstadoCitaNotAndIdNot(
                paciente.getId(), medico.getEspecialidad().getId(), fecha, estadoCancelada, idExcluido)) {
            throw new IllegalArgumentException(ERR_CONFLICTO_ESPECIALIDAD_DIA);
        }

        if (citaMedicaRepository.existsByMedicoIdAndFechaCitaAndHoraCitaAndEstadoCitaNotAndIdNot(
                medico.getId(), fecha, hora, estadoCancelada, idExcluido)) {
            throw new IllegalArgumentException(ERR_CONFLICTO_HORARIO_MEDICO);
        }

        if (citaMedicaRepository.existsByPacienteIdAndFechaCitaAndHoraCitaAndEstadoCitaNotAndIdNot(
                paciente.getId(), fecha, hora, estadoCancelada, idExcluido)) {
            throw new IllegalArgumentException(ERR_CONFLICTO_HORARIO_PACIENTE);
        }
    }

    private boolean esReactivacionDeCitaCancelada(EstadoCitaEnum estadoActual, EstadoCitaEnum nuevoEstado) {
        return estadoActual == EstadoCitaEnum.Cancelada && nuevoEstado != EstadoCitaEnum.Cancelada;
    }

    private CitaMedica obtenerCitaParaActualizar(Integer id) {
        return citaMedicaRepository.buscarParaActualizar(id)
                .orElseThrow(() -> new EntityNotFoundException(ERR_CITA_NO_ENCONTRADA));
    }

    private Paciente bloquearPaciente(Integer id) {
        return pacienteRepository.buscarParaActualizar(id)
                .orElseThrow(() -> new EntityNotFoundException(ERR_PACIENTE_NO_ENCONTRADO));
    }

    private Medico bloquearMedico(Integer id) {
        return medicoRepository.buscarParaActualizar(id)
                .orElseThrow(() -> new EntityNotFoundException(ERR_MEDICO_NO_ENCONTRADO));
    }

    private Medico obtenerMedicoPorCorreo(String correoUsuario) {
        return medicoRepository.findByUsuarioCorreo(correoUsuario)
                .orElseThrow(() -> new EntityNotFoundException(ERR_MEDICO_USUARIO_NO_ENCONTRADO));
    }

    private CitaMedicaResponseDto mapearADto(CitaMedica cita) {
        Paciente paciente = cita.getPaciente();
        Medico medico = cita.getMedico();

        CitaMedicaResponseDto dto = new CitaMedicaResponseDto();
        dto.setId(cita.getId());
        dto.setPacienteId(paciente.getId());
        dto.setPacienteNombreCompleto(paciente.getNombre() + " " + paciente.getApellido());
        dto.setPacienteDni(paciente.getDocumentoIdentidad());

        dto.setMedicoId(medico.getId());
        dto.setMedicoNombreCompleto(PREFIJO_MEDICO + medico.getUsuario().getNombre() + " " + medico.getUsuario().getApellido());
        dto.setMedicoEspecialidad(medico.getEspecialidad().getNombre());

        dto.setFechaCita(cita.getFechaCita());
        dto.setHoraCita(cita.getHoraCita());
        dto.setEstadoCita(cita.getEstadoCita().valorApi());
        return dto;
    }
}
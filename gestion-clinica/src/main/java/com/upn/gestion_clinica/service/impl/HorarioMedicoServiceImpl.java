package com.upn.gestion_clinica.service.impl;

import com.upn.gestion_clinica.dto.horario_medico.HorarioMedicoRequestDto;
import com.upn.gestion_clinica.dto.horario_medico.HorarioMedicoResponseDto;
import com.upn.gestion_clinica.entity.HorarioMedico;
import com.upn.gestion_clinica.entity.Medico;
import com.upn.gestion_clinica.entity.CitaMedica;
import com.upn.gestion_clinica.entity.EstadoCitaEnum;
import com.upn.gestion_clinica.repository.CitaMedicaRepository;
import com.upn.gestion_clinica.repository.HorarioMedicoRepository;
import com.upn.gestion_clinica.repository.MedicoRepository;
import com.upn.gestion_clinica.service.HorarioMedicoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.stream.Collectors;

@Service
public class HorarioMedicoServiceImpl implements HorarioMedicoService {

    private final HorarioMedicoRepository horarioMedicoRepository;
    private final MedicoRepository medicoRepository;
    private final CitaMedicaRepository citaMedicaRepository;

    public HorarioMedicoServiceImpl(HorarioMedicoRepository horarioMedicoRepository,
                                   MedicoRepository medicoRepository,
                                   CitaMedicaRepository citaMedicaRepository) {
        this.horarioMedicoRepository = horarioMedicoRepository;
        this.medicoRepository = medicoRepository;
        this.citaMedicaRepository = citaMedicaRepository;
    }

    @Override
    @Transactional
    public HorarioMedicoResponseDto crear(HorarioMedicoRequestDto requestDto) {
        validarHoras(requestDto);
        validarSolapamiento(requestDto, null);

        Medico medico = medicoRepository.findById(requestDto.getMedicoId())
                .orElseThrow(() -> new EntityNotFoundException("Médico no encontrado con id: " + requestDto.getMedicoId()));

        HorarioMedico horario = new HorarioMedico();
        horario.setMedico(medico);
        horario.setDiaSemana(requestDto.getDiaSemana());
        horario.setHoraInicio(requestDto.getHoraInicio());
        horario.setHoraFin(requestDto.getHoraFin());

        HorarioMedico guardado = horarioMedicoRepository.save(horario);
        return mapearAResponseDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public HorarioMedicoResponseDto obtenerPorId(Integer id) {
        HorarioMedico horario = horarioMedicoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Horario médico no encontrado con id: " + id));
        return mapearAResponseDto(horario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HorarioMedicoResponseDto> listarTodos() {
        return horarioMedicoRepository.findAll().stream()
                .map(this::mapearAResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HorarioMedicoResponseDto> listarPorMedico(Integer medicoId) {
        return horarioMedicoRepository.findByMedicoId(medicoId).stream()
                .map(this::mapearAResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HorarioMedicoResponseDto> listarPorEspecialidad(Integer especialidadId) {
        return horarioMedicoRepository.findByMedicoEspecialidadId(especialidadId).stream()
                .map(this::mapearAResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public HorarioMedicoResponseDto actualizar(Integer id, HorarioMedicoRequestDto requestDto) {
        HorarioMedico horario = horarioMedicoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Horario médico no encontrado con id: " + id));

        validarHoras(requestDto);
        validarSolapamiento(requestDto, id);

        Medico medico = medicoRepository.findById(requestDto.getMedicoId())
                .orElseThrow(() -> new EntityNotFoundException("Médico no encontrado con id: " + requestDto.getMedicoId()));

        validarCitasFuturasCubiertas(horario.getMedico().getId(), id, requestDto);
        if (!horario.getMedico().getId().equals(requestDto.getMedicoId())) {
            validarCitasFuturasCubiertas(requestDto.getMedicoId(), null, requestDto);
        }

        horario.setMedico(medico);
        horario.setDiaSemana(requestDto.getDiaSemana());
        horario.setHoraInicio(requestDto.getHoraInicio());
        horario.setHoraFin(requestDto.getHoraFin());

        HorarioMedico actualizado = horarioMedicoRepository.save(horario);
        return mapearAResponseDto(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        HorarioMedico horario = horarioMedicoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Horario médico no encontrado con id: " + id));
        validarCitasFuturasCubiertas(horario.getMedico().getId(), id, null);
        horarioMedicoRepository.deleteById(id);
    }

    private void validarHoras(HorarioMedicoRequestDto requestDto) {
        if (requestDto.getHoraFin().isBefore(requestDto.getHoraInicio()) || 
            requestDto.getHoraFin().equals(requestDto.getHoraInicio())) {
            throw new IllegalArgumentException("La hora de fin debe ser estrictamente mayor a la hora de inicio.");
        }
    }

    private void validarSolapamiento(HorarioMedicoRequestDto requestDto, Integer idExcluido) {
        List<HorarioMedico> existentes = horarioMedicoRepository.findByMedicoIdAndDiaSemana(requestDto.getMedicoId(), requestDto.getDiaSemana());
        for (HorarioMedico existente : existentes) {

            if (idExcluido != null && existente.getId().equals(idExcluido)) {
                continue;
            }

            boolean solapa = requestDto.getHoraInicio().isBefore(existente.getHoraFin()) &&
                             requestDto.getHoraFin().isAfter(existente.getHoraInicio());

            if (solapa) {
                throw new IllegalArgumentException("El horario se solapa con un horario existente para este médico en el mismo día.");
            }
        }
    }

    /**
     * Evita que un cambio de disponibilidad deje citas futuras Confirmadas o
     * Reprogramadas fuera de todos los horarios del médico.
     */
    private void validarCitasFuturasCubiertas(Integer medicoId, Integer horarioExcluido,
                                               HorarioMedicoRequestDto reemplazo) {
        List<HorarioMedico> horariosVigentes = horarioMedicoRepository.findByMedicoId(medicoId);
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();

        boolean existenCitasSinCobertura = citaMedicaRepository
                .findByMedicoIdAndFechaCitaGreaterThanEqual(medicoId, hoy)
                .stream()
                .filter(cita -> cita.getEstadoCita() == EstadoCitaEnum.Confirmada
                        || cita.getEstadoCita() == EstadoCitaEnum.Reprogramada)
                .filter(cita -> cita.getFechaCita().isAfter(hoy)
                        || !cita.getHoraCita().isBefore(ahora))
                .anyMatch(cita -> !estaCubierta(cita, horariosVigentes, horarioExcluido, reemplazo));

        if (existenCitasSinCobertura) {
            throw new IllegalArgumentException(
                    "No se puede modificar el horario porque dejaría citas futuras programadas fuera de la disponibilidad del médico.");
        }
    }

    private boolean estaCubierta(CitaMedica cita, List<HorarioMedico> horarios,
                                 Integer horarioExcluido, HorarioMedicoRequestDto reemplazo) {
        int diaCita = cita.getFechaCita().getDayOfWeek().getValue();
        boolean cubiertaPorHorarioExistente = horarios.stream()
                .filter(horario -> horarioExcluido == null || !horario.getId().equals(horarioExcluido))
                .anyMatch(horario -> horario.getDiaSemana().equals(diaCita)
                        && contieneHora(horario.getHoraInicio(), horario.getHoraFin(), cita.getHoraCita()));

        return cubiertaPorHorarioExistente || (reemplazo != null
                && reemplazo.getMedicoId().equals(cita.getMedico().getId())
                && reemplazo.getDiaSemana().equals(diaCita)
                && contieneHora(reemplazo.getHoraInicio(), reemplazo.getHoraFin(), cita.getHoraCita()));
    }

    private boolean contieneHora(LocalTime horaInicio, LocalTime horaFin, LocalTime horaCita) {
        return !horaCita.isBefore(horaInicio) && horaCita.isBefore(horaFin);
    }

    private HorarioMedicoResponseDto mapearAResponseDto(HorarioMedico horario) {
        HorarioMedicoResponseDto dto = new HorarioMedicoResponseDto();
        dto.setId(horario.getId());
        dto.setMedicoId(horario.getMedico().getId());
        if (horario.getMedico().getUsuario() != null) {
            dto.setMedicoNombreCompleto(horario.getMedico().getUsuario().getNombre() + " " + horario.getMedico().getUsuario().getApellido());
        }
        dto.setDiaSemana(horario.getDiaSemana());
        dto.setHoraInicio(horario.getHoraInicio());
        dto.setHoraFin(horario.getHoraFin());
        return dto;
    }
}

package com.upn.gestion_clinica.service.impl;

import com.upn.gestion_clinica.dto.cita.CitaMedicaRequestDto;
import com.upn.gestion_clinica.dto.cita.CitaMedicaResponseDto;
import com.upn.gestion_clinica.entity.CitaMedica;
import com.upn.gestion_clinica.entity.EstadoCitaEnum;
import com.upn.gestion_clinica.entity.Medico;
import com.upn.gestion_clinica.entity.Paciente;
import com.upn.gestion_clinica.repository.CitaMedicaRepository;
import com.upn.gestion_clinica.repository.MedicoRepository;
import com.upn.gestion_clinica.repository.PacienteRepository;
import com.upn.gestion_clinica.service.CitaMedicaService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CitaMedicaServiceImpl implements CitaMedicaService {

    private final CitaMedicaRepository citaMedicaRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;

    public CitaMedicaServiceImpl(CitaMedicaRepository citaMedicaRepository, 
                                 PacienteRepository pacienteRepository, 
                                 MedicoRepository medicoRepository) {
        this.citaMedicaRepository = citaMedicaRepository;
        this.pacienteRepository = pacienteRepository;
        this.medicoRepository = medicoRepository;
    }

    @Override
    @Transactional
    public CitaMedicaResponseDto agendarCita(CitaMedicaRequestDto requestDto) {

        Paciente paciente = pacienteRepository.findById(requestDto.getPacienteId())
                .orElseThrow(() -> new EntityNotFoundException("Paciente no encontrado"));


        Medico medico = medicoRepository.findById(requestDto.getMedicoId())
                .orElseThrow(() -> new EntityNotFoundException("Médico no encontrado"));


        if (citaMedicaRepository.existsByMedicoIdAndFechaCitaAndHoraCita(
                requestDto.getMedicoId(), requestDto.getFechaCita(), requestDto.getHoraCita())) {
            throw new IllegalArgumentException("El médico ya tiene una cita asignada en ese horario exacto.");
        }

        if (citaMedicaRepository.existsByPacienteIdAndFechaCitaAndHoraCita(
                requestDto.getPacienteId(), requestDto.getFechaCita(), requestDto.getHoraCita())) {
            throw new IllegalArgumentException("El paciente ya tiene una cita asignada en ese horario exacto.");
        }

        CitaMedica cita = new CitaMedica();
        cita.setPaciente(paciente);
        cita.setMedico(medico);
        cita.setFechaCita(requestDto.getFechaCita());
        cita.setHoraCita(requestDto.getHoraCita());
        cita.setEstadoCita(EstadoCitaEnum.Confirmada);

        CitaMedica guardada = citaMedicaRepository.save(cita);
        return mapearADto(guardada);
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
        CitaMedica cita = citaMedicaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cita médica no encontrada"));


        boolean cambioHorario = !cita.getFechaCita().equals(requestDto.getFechaCita()) || 
                                !cita.getHoraCita().equals(requestDto.getHoraCita());
        boolean cambioMedico = !cita.getMedico().getId().equals(requestDto.getMedicoId());

        if (cambioHorario || cambioMedico) {
            if (citaMedicaRepository.existsByMedicoIdAndFechaCitaAndHoraCita(
                    requestDto.getMedicoId(), requestDto.getFechaCita(), requestDto.getHoraCita())) {
                throw new IllegalArgumentException("El médico seleccionado ya tiene una cita en ese horario.");
            }
        }

        boolean cambioPaciente = !cita.getPaciente().getId().equals(requestDto.getPacienteId());

        if (cambioHorario || cambioPaciente) {
            if (citaMedicaRepository.existsByPacienteIdAndFechaCitaAndHoraCita(
                    requestDto.getPacienteId(), requestDto.getFechaCita(), requestDto.getHoraCita())) {
                throw new IllegalArgumentException("El paciente seleccionado ya tiene una cita en ese horario.");
            }
        }

        if (cambioMedico) {
            Medico medico = medicoRepository.findById(requestDto.getMedicoId())
                    .orElseThrow(() -> new EntityNotFoundException("Médico no encontrado"));
            cita.setMedico(medico);
        }


        if (!cita.getPaciente().getId().equals(requestDto.getPacienteId())) {
            Paciente paciente = pacienteRepository.findById(requestDto.getPacienteId())
                    .orElseThrow(() -> new EntityNotFoundException("Paciente no encontrado"));
            cita.setPaciente(paciente);
        }

        cita.setFechaCita(requestDto.getFechaCita());
        cita.setHoraCita(requestDto.getHoraCita());
        

        if (cita.getEstadoCita() != EstadoCitaEnum.Asistió && cita.getEstadoCita() != EstadoCitaEnum.Cancelada) {
            cita.setEstadoCita(EstadoCitaEnum.Reprogramada);
        }

        return mapearADto(citaMedicaRepository.save(cita));
    }

    @Override
    @Transactional
    public void cambiarEstadoCita(Integer id, EstadoCitaEnum nuevoEstado) {
        CitaMedica cita = citaMedicaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cita médica no encontrada"));
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
        dto.setEstadoCita(cita.getEstadoCita().name().replace("_", " "));
        return dto;
    }
}

package com.upn.gestion_clinica.service.impl;

import com.upn.gestion_clinica.dto.Cie10ResponseDto;
import com.upn.gestion_clinica.dto.atencion.AtencionMedicaRegistroDto;
import com.upn.gestion_clinica.dto.atencion.AtencionMedicaResponseDto;
import com.upn.gestion_clinica.dto.receta.DetalleRecetaDto;
import com.upn.gestion_clinica.dto.receta.RecetaMedicaDto;
import com.upn.gestion_clinica.entity.*;
import com.upn.gestion_clinica.repository.*;
import com.upn.gestion_clinica.service.AtencionMedicaService;
import com.upn.gestion_clinica.service.CitaMedicaService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AtencionMedicaServiceImpl implements AtencionMedicaService {

    private final AtencionMedicaRepository atencionMedicaRepository;
    private final CitaMedicaRepository citaMedicaRepository;
    private final Cie10Repository cie10Repository;
    private final UsuarioRepository usuarioRepository;
    private final MedicoRepository medicoRepository;
    private final CitaMedicaService citaMedicaService;
    
    private final RecetaMedicaRepository recetaMedicaRepository;
    private final ProcedimientoMedicoRepository procedimientoMedicoRepository;
    private final MedicamentoRepository medicamentoRepository;

    public AtencionMedicaServiceImpl(AtencionMedicaRepository atencionMedicaRepository,
                                     CitaMedicaRepository citaMedicaRepository,
                                     Cie10Repository cie10Repository,
                                     UsuarioRepository usuarioRepository,
                                     MedicoRepository medicoRepository,
                                     CitaMedicaService citaMedicaService,
                                     RecetaMedicaRepository recetaMedicaRepository,
                                     ProcedimientoMedicoRepository procedimientoMedicoRepository,
                                     MedicamentoRepository medicamentoRepository) {
        this.atencionMedicaRepository = atencionMedicaRepository;
        this.citaMedicaRepository = citaMedicaRepository;
        this.cie10Repository = cie10Repository;
        this.usuarioRepository = usuarioRepository;
        this.medicoRepository = medicoRepository;
        this.citaMedicaService = citaMedicaService;
        this.recetaMedicaRepository = recetaMedicaRepository;
        this.procedimientoMedicoRepository = procedimientoMedicoRepository;
        this.medicamentoRepository = medicamentoRepository;
    }

    private Medico obtenerMedicoPorCorreo(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        return medicoRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException("El usuario logueado no es un médico válido"));
    }

    @Override
    @Transactional
    public AtencionMedicaResponseDto registrarAtencion(AtencionMedicaRegistroDto requestDto, String correoMedicoLogueado) {
        Medico medicoLogueado = obtenerMedicoPorCorreo(correoMedicoLogueado);

        CitaMedica cita = citaMedicaRepository.findById(requestDto.getCitaMedicaId())
                .orElseThrow(() -> new EntityNotFoundException("Cita médica no encontrada"));


        if (!cita.getMedico().getId().equals(medicoLogueado.getId())) {
            throw new IllegalArgumentException("No puedes registrar atención en una cita que no te pertenece");
        }


        if (cita.getEstadoCita() == EstadoCitaEnum.Cancelada || cita.getEstadoCita() == EstadoCitaEnum.No_Asistió) {
            throw new IllegalArgumentException("No se puede atender una cita Cancelada o con inasistencia");
        }
        

        if (atencionMedicaRepository.findByCitaMedicaId(cita.getId()).isPresent()) {
            throw new IllegalArgumentException("Esta cita ya cuenta con una atención médica registrada");
        }

        Cie10 cie10 = cie10Repository.findById(requestDto.getCodigoCie10())
                .orElseThrow(() -> new EntityNotFoundException("Código CIE-10 no válido"));

        AtencionMedica atencion = new AtencionMedica();
        atencion.setCitaMedica(cita);
        atencion.setMotivoConsulta(requestDto.getMotivoConsulta());
        atencion.setDiagnostico(requestDto.getDiagnostico());
        atencion.setCie10(cie10);

        AtencionMedica guardada = atencionMedicaRepository.save(atencion);


        citaMedicaService.cambiarEstadoCita(cita.getId(), EstadoCitaEnum.Asistió);


        if (requestDto.getReceta() != null) {
            RecetaMedica receta = new RecetaMedica();
            receta.setAtencionMedica(guardada);
            receta.setIndicacionesGenerales(requestDto.getReceta().getIndicacionesGenerales());
            
            if (requestDto.getReceta().getDetalles() != null && !requestDto.getReceta().getDetalles().isEmpty()) {
                for (DetalleRecetaDto detalleDto : requestDto.getReceta().getDetalles()) {
                    Medicamento medicamento = medicamentoRepository.findById(detalleDto.getMedicamentoId())
                            .orElseThrow(() -> new EntityNotFoundException("Medicamento no encontrado con ID: " + detalleDto.getMedicamentoId()));
                    
                    DetalleReceta detalle = new DetalleReceta();
                    detalle.setMedicamento(medicamento);
                    detalle.setDosis(detalleDto.getDosis());
                    detalle.setFrecuencia(detalleDto.getFrecuencia());
                    detalle.setDuracionTratamiento(detalleDto.getDuracionTratamiento());
                    detalle.setCantidadPrescrita(detalleDto.getCantidadPrescrita());
                    receta.addDetalle(detalle);
                }
            }
            recetaMedicaRepository.save(receta);
        }


        if (requestDto.getProcedimiento() != null) {
            ProcedimientoMedico procedimiento = new ProcedimientoMedico();
            procedimiento.setAtencionMedica(guardada);
            procedimiento.setDescripcionProcedimiento(requestDto.getProcedimiento().getDescripcionProcedimiento());
            procedimiento.setResultado(requestDto.getProcedimiento().getResultado());
            procedimientoMedicoRepository.save(procedimiento);
        }

        return mapearADto(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AtencionMedicaResponseDto> listarMisAtenciones(String correoMedicoLogueado) {
        Medico medico = obtenerMedicoPorCorreo(correoMedicoLogueado);
        return atencionMedicaRepository.findByCitaMedicaMedicoIdOrderByFechaAtencionDesc(medico.getId())
                .stream().map(this::mapearADto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AtencionMedicaResponseDto obtenerAtencion(Integer id, String correoMedicoLogueado) {
        Medico medico = obtenerMedicoPorCorreo(correoMedicoLogueado);
        AtencionMedica atencion = atencionMedicaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Atención médica no encontrada"));

        if (!atencion.getCitaMedica().getMedico().getId().equals(medico.getId())) {
            throw new IllegalArgumentException("No tienes permiso para ver esta atención");
        }

        return mapearADto(atencion);
    }

    private AtencionMedicaResponseDto mapearADto(AtencionMedica atencion) {
        AtencionMedicaResponseDto dto = new AtencionMedicaResponseDto();
        dto.setId(atencion.getId());
        dto.setCitaMedicaId(atencion.getCitaMedica().getId());
        
        Paciente paciente = atencion.getCitaMedica().getPaciente();
        dto.setPacienteNombreCompleto(paciente.getNombre() + " " + paciente.getApellido());
        dto.setPacienteDocumento(paciente.getDocumentoIdentidad());
        
        Usuario usuarioMedico = atencion.getCitaMedica().getMedico().getUsuario();
        dto.setMedicoNombreCompleto("Dr/Dra. " + usuarioMedico.getNombre() + " " + usuarioMedico.getApellido());
        dto.setMedicoColegiatura(atencion.getCitaMedica().getMedico().getNumeroColegiatura());
        
        dto.setMotivoConsulta(atencion.getMotivoConsulta());
        dto.setDiagnostico(atencion.getDiagnostico());
        
        Cie10ResponseDto cieDto = new Cie10ResponseDto();
        cieDto.setCodigo(atencion.getCie10().getCodigo());
        cieDto.setDescripcion(atencion.getCie10().getDescripcion());
        dto.setCie10(cieDto);
        
        dto.setFechaAtencion(atencion.getFechaAtencion());

        if (atencion.getRecetas() != null && !atencion.getRecetas().isEmpty()) {
            dto.setRecetas(atencion.getRecetas().stream().map(r -> {
                RecetaMedicaDto recetaDto = new RecetaMedicaDto();
                recetaDto.setIndicacionesGenerales(r.getIndicacionesGenerales());
                if (r.getDetalles() != null) {
                    recetaDto.setDetalles(r.getDetalles().stream().map(d -> {
                        DetalleRecetaDto detalleDto = new  DetalleRecetaDto();
                        detalleDto.setMedicamentoNombre(d.getMedicamento().getNombreComercial());
                        detalleDto.setMedicamentoPrincipioActivo(d.getMedicamento().getPrincipioActivo());
                        detalleDto.setDosis(d.getDosis());
                        detalleDto.setFrecuencia(d.getFrecuencia());
                        detalleDto.setDuracionTratamiento(d.getDuracionTratamiento());
                        detalleDto.setCantidadPrescrita(d.getCantidadPrescrita());
                        return detalleDto;
                    }).collect(java.util.stream.Collectors.toList()));
                }
                return recetaDto;
            }).collect(java.util.stream.Collectors.toList()));
        }

        if (atencion.getProcedimientos() != null && !atencion.getProcedimientos().isEmpty()) {
            dto.setProcedimientos(atencion.getProcedimientos().stream().map(p -> {
                AtencionMedicaResponseDto.ProcedimientoDto procDto = new AtencionMedicaResponseDto.ProcedimientoDto();
                procDto.setDescripcionProcedimiento(p.getDescripcionProcedimiento());
                procDto.setResultado(p.getResultado());
                return procDto;
            }).collect(java.util.stream.Collectors.toList()));
        }

        return dto;
    }
}

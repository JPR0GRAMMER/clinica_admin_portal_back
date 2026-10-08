package com.upn.gestion_clinica.service.impl;

import com.upn.gestion_clinica.dto.ProcedimientoMedicoDto;
import com.upn.gestion_clinica.dto.atencion.AtencionMedicaRegistroDto;
import com.upn.gestion_clinica.dto.atencion.AtencionMedicaResponseDto;
import com.upn.gestion_clinica.dto.atencion.HistorialClinicoResponseDto;
import com.upn.gestion_clinica.dto.receta.DetalleRecetaDto;
import com.upn.gestion_clinica.dto.receta.RecetaMedicaDto;
import com.upn.gestion_clinica.entity.AtencionMedica;
import com.upn.gestion_clinica.entity.Cie10;
import com.upn.gestion_clinica.entity.CitaMedica;
import com.upn.gestion_clinica.entity.EstadoCitaEnum;
import com.upn.gestion_clinica.entity.Especialidad;
import com.upn.gestion_clinica.entity.Medico;
import com.upn.gestion_clinica.entity.Medicamento;
import com.upn.gestion_clinica.entity.Paciente;
import com.upn.gestion_clinica.entity.Usuario;
import com.upn.gestion_clinica.repository.AtencionMedicaRepository;
import com.upn.gestion_clinica.repository.Cie10Repository;
import com.upn.gestion_clinica.repository.CitaMedicaRepository;
import com.upn.gestion_clinica.repository.MedicamentoRepository;
import com.upn.gestion_clinica.repository.MedicoRepository;
import com.upn.gestion_clinica.repository.ProcedimientoMedicoRepository;
import com.upn.gestion_clinica.repository.RecetaMedicaRepository;
import com.upn.gestion_clinica.repository.UsuarioRepository;
import com.upn.gestion_clinica.service.CitaMedicaService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtencionMedicaServiceImplTest {

    @Mock
    private AtencionMedicaRepository atencionMedicaRepository;

    @Mock
    private CitaMedicaRepository citaMedicaRepository;

    @Mock
    private Cie10Repository cie10Repository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private MedicoRepository medicoRepository;

    @Mock
    private CitaMedicaService citaMedicaService;

    @Mock
    private RecetaMedicaRepository recetaMedicaRepository;

    @Mock
    private ProcedimientoMedicoRepository procedimientoMedicoRepository;

    @Mock
    private MedicamentoRepository medicamentoRepository;

    @InjectMocks
    private AtencionMedicaServiceImpl atencionService;

    @Test
    @DisplayName("AtencionMedicaServiceImpl.registrarAtencion() - registra atención válida sin opcionales")
    void registrarAtencion_RegistraAtencionValida() {
        Medico medico = medico(10);
        CitaMedica cita = cita(100, medico, EstadoCitaEnum.Confirmada);
        Cie10 cie10 = cie10("J06.9");
        AtencionMedicaRegistroDto request = request(100, "Dolor de garganta", "Faringitis", "J06.9");
        AtencionMedica guardada = atencion(500, cita, cie10);
        prepararRegistro(medico, cita, cie10, request, guardada);

        AtencionMedicaResponseDto response = atencionService.registrarAtencion(request, "medico@clinica.com");

        assertEquals(500, response.getId());
        assertEquals(100, response.getCitaMedicaId());
        assertEquals("J06.9", response.getCie10().getCodigo());
        assertEquals("Diagnóstico", response.getDiagnostico());
        verify(citaMedicaService).cambiarEstadoCita(100, EstadoCitaEnum.Asistió);
        verify(recetaMedicaRepository, never()).save(any());
        verify(procedimientoMedicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("AtencionMedicaServiceImpl.registrarAtencion() - rechaza cita de otro médico")
    void registrarAtencion_RechazaCitaDeOtroMedico() {
        Medico medicoLogueado = medico(10);
        Medico otroMedico = medico(11);
        CitaMedica cita = cita(100, otroMedico, EstadoCitaEnum.Confirmada);
        AtencionMedicaRegistroDto request = request(100, "Consulta", "Diagnóstico", "J06.9");
        when(usuarioRepository.findByCorreo("medico@clinica.com")).thenReturn(Optional.of(medicoLogueado.getUsuario()));
        when(medicoRepository.findByUsuarioId(10)).thenReturn(Optional.of(medicoLogueado));
        when(citaMedicaRepository.findById(100)).thenReturn(Optional.of(cita));

        assertThrows(IllegalArgumentException.class,
                () -> atencionService.registrarAtencion(request, "medico@clinica.com"));
        verify(atencionMedicaRepository, never()).save(any());
    }

    @Test
    @DisplayName("AtencionMedicaServiceImpl.registrarAtencion() - rechaza cita cancelada o no asistida")
    void registrarAtencion_RechazaEstadosNoAtendibles() {
        Medico medico = medico(10);
        AtencionMedicaRegistroDto request = request(100, "Consulta", "Diagnóstico", "J06.9");
        when(usuarioRepository.findByCorreo("medico@clinica.com")).thenReturn(Optional.of(medico.getUsuario()));
        when(medicoRepository.findByUsuarioId(10)).thenReturn(Optional.of(medico));

        for (EstadoCitaEnum estado : List.of(EstadoCitaEnum.Cancelada, EstadoCitaEnum.No_Asistió)) {
            CitaMedica cita = cita(100, medico, estado);
            when(citaMedicaRepository.findById(100)).thenReturn(Optional.of(cita));

            assertThrows(IllegalArgumentException.class,
                    () -> atencionService.registrarAtencion(request, "medico@clinica.com"));
        }

        verify(atencionMedicaRepository, never()).save(any());
    }

    @Test
    @DisplayName("AtencionMedicaServiceImpl.registrarAtencion() - rechaza atención duplicada")
    void registrarAtencion_RechazaDuplicada() {
        Medico medico = medico(10);
        CitaMedica cita = cita(100, medico, EstadoCitaEnum.Confirmada);
        AtencionMedicaRegistroDto request = request(100, "Consulta", "Diagnóstico", "J06.9");
        prepararMedicoYCita(medico, cita);
        when(atencionMedicaRepository.findByCitaMedicaId(100)).thenReturn(Optional.of(new AtencionMedica()));

        assertThrows(IllegalArgumentException.class,
                () -> atencionService.registrarAtencion(request, "medico@clinica.com"));
        verify(cie10Repository, never()).findById(any());
    }

    @Test
    @DisplayName("AtencionMedicaServiceImpl.registrarAtencion() - rechaza CIE-10 inválido")
    void registrarAtencion_RechazaCie10Invalido() {
        Medico medico = medico(10);
        CitaMedica cita = cita(100, medico, EstadoCitaEnum.Confirmada);
        AtencionMedicaRegistroDto request = request(100, "Consulta", "Diagnóstico", "X99.9");
        prepararMedicoYCita(medico, cita);
        when(atencionMedicaRepository.findByCitaMedicaId(100)).thenReturn(Optional.empty());
        when(cie10Repository.findById("X99.9")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> atencionService.registrarAtencion(request, "medico@clinica.com"));
        verify(atencionMedicaRepository, never()).save(any());
    }

    @Test
    @DisplayName("AtencionMedicaServiceImpl.registrarAtencion() - guarda receta, medicamento y procedimiento opcionales")
    void registrarAtencion_GuardaRecetaMedicamentoYProcedimiento() {
        Medico medico = medico(10);
        CitaMedica cita = cita(100, medico, EstadoCitaEnum.Confirmada);
        Cie10 cie10 = cie10("J06.9");
        AtencionMedicaRegistroDto request = request(100, "Consulta", "Diagnóstico", "J06.9");

        RecetaMedicaDto receta = new RecetaMedicaDto();
        receta.setIndicacionesGenerales("Tomar después de los alimentos");
        DetalleRecetaDto detalle = new DetalleRecetaDto();
        detalle.setMedicamentoId(900L);
        detalle.setDosis("500 mg");
        detalle.setFrecuencia("Cada 8 horas");
        detalle.setDuracionTratamiento("5 días");
        detalle.setCantidadPrescrita(15);
        receta.setDetalles(List.of(detalle));
        request.setReceta(receta);

        ProcedimientoMedicoDto procedimiento = new ProcedimientoMedicoDto();
        procedimiento.setDescripcionProcedimiento("Evaluación clínica");
        procedimiento.setResultado("Sin hallazgos adicionales");
        request.setProcedimiento(procedimiento);

        AtencionMedica guardada = atencion(500, cita, cie10);
        Medicamento medicamento = new Medicamento();
        medicamento.setId(900L);
        medicamento.setNombreComercial("Medicamento de prueba");
        prepararRegistro(medico, cita, cie10, request, guardada);
        when(medicamentoRepository.findById(900L)).thenReturn(Optional.of(medicamento));

        AtencionMedicaResponseDto response = atencionService.registrarAtencion(request, "medico@clinica.com");

        assertEquals(500, response.getId());
        verify(recetaMedicaRepository).save(any());
        verify(procedimientoMedicoRepository).save(any());
        verify(medicamentoRepository).findById(900L);
    }

    @Test
    @DisplayName("AtencionMedicaServiceImpl.registrarAtencion() - medicamento de receta inexistente")
    void registrarAtencion_RechazaMedicamentoInexistente() {
        Medico medico = medico(10);
        CitaMedica cita = cita(100, medico, EstadoCitaEnum.Confirmada);
        Cie10 cie10 = cie10("J06.9");
        AtencionMedicaRegistroDto request = request(100, "Consulta", "Diagnóstico", "J06.9");
        RecetaMedicaDto receta = new RecetaMedicaDto();
        DetalleRecetaDto detalle = new DetalleRecetaDto();
        detalle.setMedicamentoId(999L);
        receta.setDetalles(List.of(detalle));
        request.setReceta(receta);
        AtencionMedica guardada = atencion(500, cita, cie10);
        prepararRegistro(medico, cita, cie10, request, guardada);
        when(medicamentoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> atencionService.registrarAtencion(request, "medico@clinica.com"));
        verify(recetaMedicaRepository, never()).save(any());
    }

    @Test
    @DisplayName("AtencionMedicaServiceImpl.listarMisAtenciones() - lista las atenciones del médico autenticado")
    void listarMisAtenciones_ListaAtencionesDelMedico() {
        Medico medico = medico(10);
        CitaMedica cita = cita(100, medico, EstadoCitaEnum.Asistió);
        AtencionMedica atencion = atencion(500, cita, cie10("J06.9"));
        when(usuarioRepository.findByCorreo("medico@clinica.com")).thenReturn(Optional.of(medico.getUsuario()));
        when(medicoRepository.findByUsuarioId(10)).thenReturn(Optional.of(medico));
        when(atencionMedicaRepository.findByCitaMedicaMedicoIdOrderByFechaAtencionDesc(10))
                .thenReturn(List.of(atencion));

        List<AtencionMedicaResponseDto> response = atencionService.listarMisAtenciones("medico@clinica.com");

        assertEquals(1, response.size());
        assertEquals(500, response.get(0).getId());
        verify(atencionMedicaRepository).findByCitaMedicaMedicoIdOrderByFechaAtencionDesc(10);
    }

    @Test
    @DisplayName("AtencionMedicaServiceImpl.listarMisAtenciones() - devuelve lista vacía")
    void listarMisAtenciones_DevuelveListaVacia() {
        Medico medico = medico(10);
        when(usuarioRepository.findByCorreo("medico@clinica.com")).thenReturn(Optional.of(medico.getUsuario()));
        when(medicoRepository.findByUsuarioId(10)).thenReturn(Optional.of(medico));
        when(atencionMedicaRepository.findByCitaMedicaMedicoIdOrderByFechaAtencionDesc(10)).thenReturn(List.of());

        assertTrue(atencionService.listarMisAtenciones("medico@clinica.com").isEmpty());
    }

    @Test
    @DisplayName("AtencionMedicaServiceImpl.listarMisAtenciones() - usuario inexistente")
    void listarMisAtenciones_RechazaUsuarioInexistente() {
        when(usuarioRepository.findByCorreo("desconocido@clinica.com")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> atencionService.listarMisAtenciones("desconocido@clinica.com"));
    }

    @Test
    @DisplayName("AtencionMedicaServiceImpl.listarMisAtenciones() - usuario que no es médico")
    void listarMisAtenciones_RechazaUsuarioNoMedico() {
        Usuario usuario = new Usuario();
        usuario.setId(99);
        when(usuarioRepository.findByCorreo("paciente@clinica.com")).thenReturn(Optional.of(usuario));
        when(medicoRepository.findByUsuarioId(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> atencionService.listarMisAtenciones("paciente@clinica.com"));
    }

    private void prepararRegistro(Medico medico, CitaMedica cita, Cie10 cie10,
                                  AtencionMedicaRegistroDto request, AtencionMedica guardada) {
        prepararMedicoYCita(medico, cita);
        when(atencionMedicaRepository.findByCitaMedicaId(request.getCitaMedicaId())).thenReturn(Optional.empty());
        when(cie10Repository.findById(request.getCodigoCie10())).thenReturn(Optional.of(cie10));
        when(atencionMedicaRepository.save(any(AtencionMedica.class))).thenReturn(guardada);
    }

    private void prepararMedicoYCita(Medico medico, CitaMedica cita) {
        when(usuarioRepository.findByCorreo("medico@clinica.com")).thenReturn(Optional.of(medico.getUsuario()));
        when(medicoRepository.findByUsuarioId(medico.getId())).thenReturn(Optional.of(medico));
        when(citaMedicaRepository.findById(cita.getId())).thenReturn(Optional.of(cita));
    }

    private AtencionMedicaRegistroDto request(Integer citaId, String motivo, String diagnostico, String codigo) {
        AtencionMedicaRegistroDto request = new AtencionMedicaRegistroDto();
        request.setCitaMedicaId(citaId);
        request.setMotivoConsulta(motivo);
        request.setDiagnostico(diagnostico);
        request.setCodigoCie10(codigo);
        return request;
    }

    private Medico medico(Integer id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Roberto");
        usuario.setApellido("Sanchez");
        usuario.setCorreo("medico@clinica.com");
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre("Medicina General");
        Medico medico = new Medico();
        medico.setId(id);
        medico.setUsuario(usuario);
        medico.setEspecialidad(especialidad);
        medico.setNumeroColegiatura("CMP-" + id);
        return medico;
    }

    private Paciente paciente() {
        Paciente paciente = new Paciente();
        paciente.setId(1);
        paciente.setNombre("Carlos");
        paciente.setApellido("Mendoza");
        paciente.setDocumentoIdentidad("72819283");
        return paciente;
    }

    private CitaMedica cita(Integer id, Medico medico, EstadoCitaEnum estado) {
        CitaMedica cita = new CitaMedica();
        cita.setId(id);
        cita.setMedico(medico);
        cita.setPaciente(paciente());
        cita.setEstadoCita(estado);
        return cita;
    }

    private Cie10 cie10(String codigo) {
        Cie10 cie10 = new Cie10();
        cie10.setCodigo(codigo);
        cie10.setDescripcion("Infección respiratoria aguda");
        return cie10;
    }

    private AtencionMedica atencion(Integer id, CitaMedica cita, Cie10 cie10) {
        AtencionMedica atencion = new AtencionMedica();
        atencion.setId(id);
        atencion.setCitaMedica(cita);
        atencion.setMotivoConsulta("Consulta");
        atencion.setDiagnostico("Diagnóstico");
        atencion.setCie10(cie10);
        return atencion;
    }
}

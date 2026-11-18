package com.upn.gestion_clinica;

import com.upn.gestion_clinica.entity.*;
import com.upn.gestion_clinica.repository.*;
import com.upn.gestion_clinica.security.JwtUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("tc12")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MedCoreFuncionalTest {
    @Autowired  ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtUtil jwt;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired RolRepository roles;
    @Autowired UsuarioRepository usuarios;
    @Autowired EspecialidadRepository especialidades;
    @Autowired MedicoRepository medicos;
    @Autowired PacienteRepository pacientes;
    @Autowired Cie10Repository diagnosticos;
    @Autowired HorarioMedicoRepository horarios;
    @Autowired CitaMedicaRepository citas;
    @Autowired AtencionMedicaRepository atenciones;
    @Autowired MedicamentoRepository medicamentos;
    @LocalServerPort int port;

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final LocalDate fecha = LocalDate.now().plusDays(7);
    private int secuencia = (int) ((System.currentTimeMillis() / 100) % 80000000L) + 10000000;
    private Paciente paciente;
    private Medico medico;
    private Medico otroMedico;
    private Usuario recepcion;
    private HorarioMedico horario;

    @BeforeEach
    void preparar() {
        recepcion = usuario("RECEPCIONISTA");
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre("Especialidad TC12 " + secuencia++);
        especialidades.save(especialidad);
        medico = medico(especialidad);
        otroMedico = medico(especialidad);
        paciente = paciente();
        horario = horario(medico);
        horario(otroMedico);
        if (!diagnosticos.existsById("J00")) {
            Cie10 diagnostico = new Cie10();
            diagnostico.setCodigo("J00");
            diagnostico.setDescripcion("Diagnóstico de prueba");
            diagnosticos.save(diagnostico);
        }
    }

    @AfterAll
    void limpiarDatosDePrueba() {
        jdbc.update("""
            DELETE FROM detalle_receta WHERE receta_medica_id IN (
                SELECT r.id FROM receta_medica r
                JOIN atencion_medica a ON r.atencion_medica_id = a.id
                JOIN cita_medica c ON a.cita_medica_id = c.id
                JOIN paciente p ON c.paciente_id = p.id
                WHERE p.correo = 'paciente@example.test'
            )
        """);
        jdbc.update("""
            DELETE FROM receta_medica WHERE atencion_medica_id IN (
                SELECT a.id FROM atencion_medica a
                JOIN cita_medica c ON a.cita_medica_id = c.id
                JOIN paciente p ON c.paciente_id = p.id
                WHERE p.correo = 'paciente@example.test'
            )
        """);
        jdbc.update("""
            DELETE FROM atencion_medica WHERE cita_medica_id IN (
                SELECT c.id FROM cita_medica c
                JOIN paciente p ON c.paciente_id = p.id
                WHERE p.correo = 'paciente@example.test'
            )
        """);
        jdbc.update("""
            DELETE FROM cita_medica
            WHERE paciente_id IN (SELECT id FROM paciente WHERE correo = 'paciente@example.test')
            OR medico_id IN (SELECT m.id FROM medico m JOIN usuario u ON m.usuario_id = u.id WHERE u.correo LIKE 'prueba-%@example.test')
        """);
        jdbc.update("""
            DELETE FROM horario_medico
            WHERE medico_id IN (SELECT m.id FROM medico m JOIN usuario u ON m.usuario_id = u.id WHERE u.correo LIKE 'prueba-%@example.test')
        """);
        jdbc.update("DELETE FROM medico WHERE usuario_id IN (SELECT id FROM usuario WHERE correo LIKE 'prueba-%@example.test')");
        jdbc.update("DELETE FROM usuario WHERE correo LIKE 'prueba-%@example.test'");
        jdbc.update("DELETE FROM paciente WHERE correo = 'paciente@example.test'");
        jdbc.update("DELETE FROM especialidad WHERE nombre LIKE '%TC12%'");
        jdbc.update("DELETE FROM medicamento WHERE codigo LIKE 'TC12-%'");
    }

    @Test void tc01_agenda_y_devuelve_confirmada() {
        int id = agendar("09:00");
        assertEquals(EstadoCitaEnum.Confirmada, citas.findById(id).orElseThrow().getEstadoCita());
        assertEquals(EstadoCitaEnum.Confirmada.name(), verificar(200,
                enviar("GET", "/api/citas/" + id, null, recepcion)).path("estadoCita").asString());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "TC12_FRONTEND_DIR", matches = ".+")
    void navegador_agenda_en_el_ultimo_bloque_con_api_y_base_reales() throws Exception {
        String clave = "MedCore-fixture-12";
        recepcion.setContrasena(passwordEncoder.encode(clave));
        usuarios.save(recepcion);
        Path frontend = Path.of(System.getenv("TC12_FRONTEND_DIR"));
        var proceso = new ProcessBuilder("node", frontend.resolve("e2e/tc12-agendamiento.mjs").toString(),
                "http://127.0.0.1:" + port, recepcion.getCorreo(), clave, paciente.getDocumentoIdentidad(),
                medico.getEspecialidad().getNombre(), fecha.toString()).redirectErrorStream(true).start();
        if (!proceso.waitFor(45, TimeUnit.SECONDS)) {
            proceso.destroyForcibly();
            fail("La prueba de navegador excedió 45 segundos.");
        }
        String salida = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, proceso.exitValue(), salida);
        System.out.print(salida);
    }

    @Test void tc02_reprograma_y_libera_turno_previo() {
        int id = agendar("09:00");
        LocalDate nuevaFecha = fecha.plusWeeks(1);
        JsonNode respuesta = verificar(200, enviar("PUT", "/api/citas/" + id,
                solicitud(paciente, medico, nuevaFecha, "11:00"), recepcion));
        assertEquals(id, respuesta.path("id").asInt());
        CitaMedica actualizada = citas.findById(id).orElseThrow();
        assertEquals(EstadoCitaEnum.Reprogramada, actualizada.getEstadoCita());
        assertEquals(nuevaFecha, actualizada.getFechaCita());
        assertEquals(LocalTime.of(11, 0), actualizada.getHoraCita());
        verificar(201, enviar("POST", "/api/citas", solicitud(paciente(), medico, "09:00"), recepcion));
    }

    @Test void tc03_acepta_inicio_y_rechaza_hora_previa() {
        verificar(400, enviar("POST", "/api/citas", solicitud(paciente, medico, "07:00"), recepcion));
        assertEquals(LocalTime.of(8, 0), citas.findById(agendar("08:00")).orElseThrow().getHoraCita());
    }

    @Test void tc04_acepta_ultimo_bloque() {
        assertEquals(LocalTime.NOON, citas.findById(agendar("12:00")).orElseThrow().getHoraCita());
    }

    @Test void tc05_rechaza_inicio_en_hora_de_cierre() {
        verificar(400, enviar("POST", "/api/citas", solicitud(paciente, medico, "13:00"), recepcion));
    }

    @Test void tc06_no_reprograma_cita_atendida() {
        verificarEstadoFinalInmutable(EstadoCitaEnum.Asistió);
    }

    @Test void tc07_no_reprograma_cita_cancelada() {
        verificarEstadoFinalInmutable(EstadoCitaEnum.Cancelada);
    }

    @Test void tc08_rechaza_conflicto_del_medico() {
        agendar("09:00");
        assertTrue(verificar(400, enviar("POST", "/api/citas", solicitud(paciente(), medico, "09:00"), recepcion))
                .path("mensaje").asString().contains("médico"));
        assertEquals(1, citas.findAllByMedicoIdOrderByFechaCitaDescHoraCitaDesc(medico.getId()).size());
    }

    @Test void tc09_rechaza_conflicto_del_paciente_con_otro_medico() {
        agendar("09:00");
        Especialidad diferente = new Especialidad();
        diferente.setNombre("Otra especialidad TC12 " + secuencia++);
        Medico especialista = medico(especialidades.save(diferente));
        horario(especialista);
        assertTrue(verificar(400, enviar("POST", "/api/citas", solicitud(paciente, especialista, "09:00"), recepcion))
                .path("mensaje").asString().contains("paciente"));
        assertTrue(citas.findAllByMedicoIdOrderByFechaCitaDescHoraCitaDesc(especialista.getId()).isEmpty());
    }

    @Test void tc10_valida_paciente_obligatorio() {
        assertTrue(verificar(400, enviar("POST", "/api/citas",
                Map.of("medicoId", medico.getId(), "fechaCita", fecha, "horaCita", "09:00"), recepcion))
                .path("mensaje").asString().toLowerCase().contains("paciente"));
        assertTrue(citas.findAllByMedicoIdOrderByFechaCitaDescHoraCitaDesc(medico.getId()).isEmpty());
    }

    @Test void tc11_solo_una_reserva_ante_envios_simultaneos() throws Exception {
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var primera = executor.submit(() -> enviarSimultaneo(listos, inicio, paciente));
            var segunda = executor.submit(() -> enviarSimultaneo(listos, inicio, paciente));
            try {
                assertTrue(listos.await(5, TimeUnit.SECONDS));
            } finally {
                inicio.countDown();
            }
            int a = primera.get(20, TimeUnit.SECONDS);
            int b = segunda.get(20, TimeUnit.SECONDS);
            assertTrue((a == 201 && b == 400) || (a == 400 && b == 201),
                    () -> "Códigos HTTP obtenidos: " + a + ", " + b);
        }
        assertEquals(1, citas.findAllByMedicoIdOrderByFechaCitaDescHoraCitaDesc(medico.getId()).size());
    }

    @Test void tc12_el_rol_medico_no_puede_agendar_por_http() {
        verificar(403, enviar("POST", "/api/citas", solicitud(paciente, medico, "09:00"), medico.getUsuario()));
        assertTrue(citas.findAllByMedicoIdOrderByFechaCitaDescHoraCitaDesc(medico.getId()).isEmpty());
    }

    @Test void tc13_registra_atencion_y_marca_asistencia() {
        int id = agendar("09:00");
        verificar(201, atenderConReceta(id, medicamento()));
        assertTrue(atenciones.findByCitaMedicaId(id).isPresent());
        assertEquals(EstadoCitaEnum.Asistió, citas.findById(id).orElseThrow().getEstadoCita());
        JsonNode historial = historial(id);
        assertEquals(1, historial.size());
        assertEquals("J00", historial.get(0).path("cie10").path("codigo").asString());
        JsonNode recetas = historial.get(0).path("recetas");
        assertEquals(1, recetas.size());
        assertEquals(1, recetas.get(0).path("detalles").size());
    }

    @Test void tc14_rechaza_cie10_inexistente() {
        int id = agendar("09:00");
        verificar(404, atender(id, "ZZ99", medico));
        assertTrue(atenciones.findByCitaMedicaId(id).isEmpty());
        assertEquals(EstadoCitaEnum.Confirmada, citas.findById(id).orElseThrow().getEstadoCita());
    }

    @Test void tc15_medico_ajeno_no_registra_atencion() {
        int id = agendar("09:00");
        verificar(400, atender(id, "J00", otroMedico));
        assertTrue(atenciones.findByCitaMedicaId(id).isEmpty());
    }

    @Test void tc16_rechaza_atencion_duplicada() {
        int id = agendar("09:00");
        Medicamento medicamento = medicamento();
        verificar(201, atenderConReceta(id, medicamento));
        verificar(400, atenderConReceta(id, medicamento));
        JsonNode atencionesRegistradas = historial(id);
        assertEquals(1, atencionesRegistradas.size());
        assertEquals(1, atencionesRegistradas.get(0).path("recetas").size());
    }

    @Test void tc17_crea_franja_sin_cruce() {
        horarios.deleteById(horario.getId());
        int id = verificar(201, enviar("POST", "/api/horarios-medicos",
                franja("08:00", "13:00"), recepcion)).path("id").asInt();
        JsonNode franjaVisible = verificar(200, enviar("GET", "/api/horarios-medicos/" + id, null, recepcion));
        assertEquals(medico.getId(), franjaVisible.path("medicoId").asInt());
        assertEquals(LocalTime.of(8, 0), LocalTime.parse(franjaVisible.path("horaInicio").asString()));
        assertEquals(LocalTime.of(13, 0), LocalTime.parse(franjaVisible.path("horaFin").asString()));
    }

    @Test void tc18_rechaza_franjas_invertidas_o_iguales() {
        verificar(400, enviar("POST", "/api/horarios-medicos", franja("15:00", "14:00"), recepcion));
        verificar(400, enviar("POST", "/api/horarios-medicos", franja("15:00", "15:00"), recepcion));
        assertEquals(1, horarios.findByMedicoId(medico.getId()).size());
    }

    @Test void tc19_rechaza_solapamiento_parcial() {
        verificar(400, enviar("POST", "/api/horarios-medicos", franja("12:00", "16:00"), recepcion));
        assertEquals(1, horarios.findByMedicoId(medico.getId()).size());
    }

    @Test void tc20_protege_horario_con_cita_activa_y_permite_borrarlo_al_cancelar() {
        int id = agendar("09:00");
        String ruta = "/api/horarios-medicos/" + horario.getId();
        verificar(400, enviar("DELETE", ruta, null, recepcion));
        assertTrue(horarios.existsById(horario.getId()));
        assertEquals(EstadoCitaEnum.Confirmada, citas.findById(id).orElseThrow().getEstadoCita());
        cancelar(id);
        verificar(204, enviar("DELETE", ruta, null, recepcion));
        assertFalse(horarios.existsById(horario.getId()));
        assertEquals(EstadoCitaEnum.Cancelada, citas.findById(id).orElseThrow().getEstadoCita());
    }

    @Test void reprogramar_rechaza_especialidad_repetida_el_mismo_dia() {
        agendar("09:00");
        int otra = verificar(201, enviar("POST", "/api/citas",
                Map.of("pacienteId", paciente.getId(), "medicoId", otroMedico.getId(),
                        "fechaCita", fecha.plusWeeks(1), "horaCita", "09:00"), recepcion)).path("id").asInt();
        verificar(400, enviar("PUT", "/api/citas/" + otra, solicitud(paciente, otroMedico, "10:00"), recepcion));
        assertEquals(fecha.plusWeeks(1), citas.findById(otra).orElseThrow().getFechaCita());
    }

    @Test void reprogramar_permite_horario_liberado_por_cancelacion() {
        int cancelada = agendar("09:00");
        cancelar(cancelada);
        Paciente otroPaciente = paciente();
        int id = verificar(201, enviar("POST", "/api/citas", solicitud(otroPaciente, medico, "10:00"), recepcion))
                .path("id").asInt();
        verificar(200, enviar("PUT", "/api/citas/" + id, solicitud(otroPaciente, medico, "09:00"), recepcion));
    }

    private int enviarSimultaneo(CountDownLatch listos, CountDownLatch inicio, Paciente solicitante) throws InterruptedException {
        listos.countDown();
        assertTrue(inicio.await(5, TimeUnit.SECONDS));
        return enviar("POST", "/api/citas", solicitud(solicitante, medico, "09:00"), recepcion).statusCode();
    }

    private void verificarEstadoFinalInmutable(EstadoCitaEnum estado) {
        int id = agendar("09:00");
        CitaMedica cita = citas.findById(id).orElseThrow();
        cita.setEstadoCita(estado);
        citas.save(cita);
        verificar(400, enviar("PUT", "/api/citas/" + id, solicitud(paciente, otroMedico, "10:00"), recepcion));
        CitaMedica persistida = citas.findById(id).orElseThrow();
        assertEquals(estado, persistida.getEstadoCita());
        assertEquals(medico.getId(), persistida.getMedico().getId());
        assertEquals(LocalTime.of(9, 0), persistida.getHoraCita());
        assertEquals(fecha, persistida.getFechaCita());
    }

    private int agendar(String hora) {
        return verificar(201, enviar("POST", "/api/citas", solicitud(paciente, medico, hora), recepcion)).path("id").asInt();
    }

    private void cancelar(int id) {
        verificar(204, enviar("PATCH", "/api/citas/" + id + "/estado?estado=" + EstadoCitaEnum.Cancelada.name(),
                null, recepcion));
    }

    private HttpResponse<String> atender(int id, String codigo, Medico actor) {
        return enviar("POST", "/api/atenciones", Map.of("citaMedicaId", id, "codigoCie10", codigo,
                "motivoConsulta", "Motivo de prueba", "diagnostico", "Diagnóstico de prueba"), actor.getUsuario());
    }

    private HttpResponse<String> atenderConReceta(int id, Medicamento medicamento) {
        Map<String, Object> detalle = Map.of("medicamentoId", medicamento.getId(), "dosis", "1 unidad",
                "frecuencia", "Cada 24 horas", "duracionTratamiento", "1 día", "cantidadPrescrita", 1);
        return enviar("POST", "/api/atenciones", Map.of("citaMedicaId", id, "codigoCie10", "J00",
                "motivoConsulta", "Motivo de prueba", "diagnostico", "Diagnóstico de prueba",
                "receta", Map.of("indicacionesGenerales", "Indicaciones de prueba",
                        "detalles", java.util.List.of(detalle))), medico.getUsuario());
    }

    private JsonNode historial(int citaId) {
        return verificar(200, enviar("GET", "/api/atenciones/historial/citas/" + citaId, null, medico.getUsuario()))
                .path("atenciones");
    }

    private Map<String, Object> solicitud(Paciente actor, Medico especialista, String hora) {
        return solicitud(actor, especialista, fecha, hora);
    }

    private Map<String, Object> solicitud(Paciente actor, Medico especialista, LocalDate dia, String hora) {
        return Map.of("pacienteId", actor.getId(), "medicoId", especialista.getId(), "fechaCita", dia, "horaCita", hora);
    }

    private Map<String, Object> franja(String inicio, String fin) {
        return Map.of("medicoId", medico.getId(), "diaSemana", fecha.getDayOfWeek().getValue(),
                "horaInicio", inicio, "horaFin", fin);
    }

    private HttpResponse<String> enviar(String metodo, String ruta, Object cuerpo, Usuario actor) {
        String token = jwt.generateToken(User.withUsername(actor.getCorreo()).password("fixture").roles("FIXTURE").build());
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + ruta))
                .timeout(Duration.ofSeconds(12)).header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(cuerpo))).build();
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
    }

    private JsonNode verificar(int codigo, HttpResponse<String> response) {
        assertEquals(codigo, response.statusCode(), response::body);
        return response.body().isBlank() ? null : json.readTree(response.body());
    }

    private Usuario usuario(String nombreRol) {
        Rol rol = roles.findAll().stream()
                .filter(r -> nombreRol.equalsIgnoreCase(r.getNombre()))
                .findFirst()
                .orElseGet(() -> {
                    Rol nuevoRol = new Rol();
                    nuevoRol.setNombre(nombreRol);
                    nuevoRol.setDescripcion("Rol " + nombreRol);
                    nuevoRol.setEstado(1);
                    return roles.save(nuevoRol);
                });

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre("Usuario");
        usuario.setApellido("Prueba");
        usuario.setCorreo("prueba-" + secuencia++ + "@example.test");
        usuario.setContrasena("fixture");
        usuario.setEstado(1);
        return usuarios.save(usuario);
    }

    private Medico medico(Especialidad especialidad) {
        Medico nuevo = new Medico();
        nuevo.setUsuario(usuario("MEDICO"));
        nuevo.setEspecialidad(especialidad);
        nuevo.setNumeroColegiatura("CMP-" + secuencia++);
        return medicos.save(nuevo);
    }

    private Paciente paciente() {
        Paciente nuevo = new Paciente();
        nuevo.setDocumentoIdentidad(String.format("%08d", secuencia++));
        nuevo.setNombre("Paciente");
        nuevo.setApellido("Prueba");
        nuevo.setCorreo("paciente@example.test");
        nuevo.setTelefono("900000000");
        nuevo.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        nuevo.setEstado(1);
        return pacientes.save(nuevo);
    }

    private Medicamento medicamento() {
        Medicamento nuevo = new Medicamento();
        nuevo.setCodigo("TC12-" + secuencia++);
        nuevo.setNombreComercial("Medicamento de prueba");
        nuevo.setPrincipioActivo("Principio activo ficticio");
        nuevo.setPresentacion("Caja");
        nuevo.setConcentracion("1 mg");
        nuevo.setUnidadMedida("Unidad");
        nuevo.setStock(10);
        return medicamentos.save(nuevo);
    }

    private HorarioMedico horario(Medico titular) {
        HorarioMedico nuevo = new HorarioMedico();
        nuevo.setMedico(titular);
        nuevo.setDiaSemana(fecha.getDayOfWeek().getValue());
        nuevo.setHoraInicio(LocalTime.of(8, 0));
        nuevo.setHoraFin(LocalTime.of(13, 0));
        return horarios.save(nuevo);
    }
}
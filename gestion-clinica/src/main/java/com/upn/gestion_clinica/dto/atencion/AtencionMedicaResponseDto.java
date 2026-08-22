package com.upn.gestion_clinica.dto.atencion;

import com.upn.gestion_clinica.dto.Cie10ResponseDto;
import com.upn.gestion_clinica.dto.receta.RecetaMedicaDto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

import java.time.OffsetDateTime;

@Getter
@Setter
public class AtencionMedicaResponseDto {

    private Integer id;
    private Integer citaMedicaId;
    

    private String pacienteNombreCompleto;
    private String pacienteDocumento;
    

    private String medicoNombreCompleto;
    private String medicoColegiatura;

    private String motivoConsulta;
    private String diagnostico;
    
    private Cie10ResponseDto cie10;
    
    private OffsetDateTime fechaAtencion;

    private List<RecetaMedicaDto> recetas;
    private List<ProcedimientoDto> procedimientos;

    @Getter
    @Setter
    public static class ProcedimientoDto {
        private String descripcionProcedimiento;
        private String resultado;
    }
}

package com.upn.gestion_clinica.dto.atencion;
import com.upn.gestion_clinica.dto.ProcedimientoMedicoDto;
import com.upn.gestion_clinica.dto.receta.RecetaMedicaDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AtencionMedicaRegistroDto {

    @NotNull(message = "El ID de la cita médica es obligatorio.")
    private Integer citaMedicaId;

    @NotBlank(message = "El motivo de la consulta es obligatorio.")
    private String motivoConsulta;

    @NotBlank(message = "El diagnóstico es obligatorio.")
    @Size(max = 255, message = "El diagnóstico no puede exceder los 255 caracteres.")
    private String diagnostico;

    @NotBlank(message = "El código CIE-10 es obligatorio.")
    @Size(max = 7, message = "El código CIE-10 no puede exceder los 7 caracteres.")
    private String codigoCie10;


    private RecetaMedicaDto receta;
    private ProcedimientoMedicoDto procedimiento;
}

package com.upn.gestion_clinica.service;

import com.upn.gestion_clinica.dto.atencion.AtencionMedicaRegistroDto;
import com.upn.gestion_clinica.dto.atencion.AtencionMedicaResponseDto;
import java.util.List;

public interface AtencionMedicaService {
    AtencionMedicaResponseDto registrarAtencion(AtencionMedicaRegistroDto requestDto, String correoMedicoLogueado);
    List<AtencionMedicaResponseDto> listarMisAtenciones(String correoMedicoLogueado);
    AtencionMedicaResponseDto obtenerAtencion(Integer id, String correoMedicoLogueado);
}

package com.upn.gestion_clinica.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class AuthResponseDto {
    private String token;
    private String rol;
    private List<VistaDto> vistas;
}

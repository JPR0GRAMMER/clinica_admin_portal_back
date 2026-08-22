package com.upn.gestion_clinica.dto.paciente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PacienteRequestDto {

    @NotBlank(message = "El documento de identidad es obligatorio.")
    @Size(min = 8, max = 9, message = "El documento de identidad debe tener entre 8 y 9 caracteres.")
    private String documentoIdentidad;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres.")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio.")
    @Size(max = 100, message = "El apellido no puede exceder los 100 caracteres.")
    private String apellido;

    @NotNull(message = "La fecha de nacimiento es obligatoria.")
    @PastOrPresent(message = "La fecha de nacimiento debe ser una fecha pasada.")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El teléfono es obligatorio.")
    @Size(max = 15, message = "El teléfono no puede exceder los 15 caracteres.")
    private String telefono;

    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "El formato del correo es inválido.")
    @Size(max = 45, message = "El correo no puede exceder los 45 caracteres.")
    private String correo;
}

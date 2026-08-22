package com.upn.gestion_clinica.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioRegistroDto {

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres.")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio.")
    @Size(max = 100, message = "El apellido no puede exceder los 100 caracteres.")
    private String apellido;

    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "El correo debe tener un formato válido.")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria.")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres.")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$", 
             message = "La contraseña debe tener al menos 8 caracteres, una mayúscula, un número y un símbolo.")
    private String contrasena;

    @NotNull(message = "El ID del rol es obligatorio.")
    private Integer rolId;

    @jakarta.validation.Valid
    private DetallesMedicoRegistroDto detallesMedico;

    @jakarta.validation.Valid
    private DetallesFarmaceuticoDto detallesFarmaceutico;
}

package com.justiconsulta.store.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(

        @NotBlank(message = "El tipo de documento es obligatorio")
        String documentType,

        @NotBlank(message = "El número de documento es obligatorio")
        String documentNumber,

        @NotBlank(message = "El primer nombre es obligatorio")
        String firstName,

        String middleName,

        @NotBlank(message = "El primer apellido es obligatorio")
        String firstLastName,

        String secondLastName,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El formato del correo no es válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String password,

        String birthDate
) {
}
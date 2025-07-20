package com.rti.prueba.controller.dto;

import com.rti.prueba.bd.enumData.Ocupacion;
import com.rti.prueba.bd.orm.ClienteORM;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para actualizar un cliente existente
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteUpdateDTO {
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", message = "El nombre solo puede contener letras y espacios")
    private String nombre;
    
    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 150, message = "Los apellidos no pueden tener más de 150 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", message = "Los apellidos solo pueden contener letras y espacios")
    private String apellidos;
    
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser anterior a la fecha actual")
    private LocalDate fechaNacimiento;
    
    @NotBlank(message = "La ciudad es obligatoria")
    @Size(max = 100, message = "La ciudad no puede tener más de 100 caracteres")
    private String ciudad;
    
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico debe tener un formato válido")
    @Size(max = 255, message = "El correo electrónico no puede tener más de 255 caracteres")
    private String correoElectronico;
    
    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 20, message = "El teléfono no puede tener más de 20 caracteres")
    @Pattern(regexp = "^[+]?[0-9\\s-()]+$", message = "El teléfono debe tener un formato válido")
    private String telefono;
    
    @NotNull(message = "La ocupación es obligatoria")
    private Ocupacion ocupacion;
}

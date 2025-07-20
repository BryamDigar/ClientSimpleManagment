package com.rti.prueba.controller.dto;

import com.rti.prueba.enums.Ocupacion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para la respuesta de cliente
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponseDTO {
    
    private String numeroDocumento;
    private String nombre;
    private String apellidos;
    private LocalDate fechaNacimiento;
    private String ciudad;
    private String correoElectronico;
    private String telefono;
    private Ocupacion ocupacion;
    private Boolean esViable;
    private int edad;
}

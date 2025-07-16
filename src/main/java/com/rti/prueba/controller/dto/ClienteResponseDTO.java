package com.rti.prueba.controller.dto;

import com.rti.prueba.bd.orm.ClienteORM;
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
    private ClienteORM.Ocupacion ocupacion;
    private Boolean esViable;
    private int edad;

    // Método estático para crear desde ClienteORM
    public static ClienteResponseDTO fromEntity(ClienteORM cliente, int edad) {
        return new ClienteResponseDTO(
                cliente.getNumeroDocumento(),
                cliente.getNombre(),
                cliente.getApellidos(),
                cliente.getFechaNacimiento(),
                cliente.getCiudad(),
                cliente.getCorreoElectronico(),
                cliente.getTelefono(),
                cliente.getOcupacion(),
                cliente.getEsViable(),
                edad
        );
    }
}

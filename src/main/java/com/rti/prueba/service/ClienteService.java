package com.rti.prueba.service;

import com.rti.prueba.bd.jpa.ClienteJPA;
import com.rti.prueba.bd.orm.ClienteORM;
import com.rti.prueba.controller.dto.ClienteCreateDTO;
import com.rti.prueba.controller.dto.ClienteResponseDTO;
import com.rti.prueba.controller.dto.ClienteUpdateDTO;
import com.rti.prueba.exception.ClienteAlreadyExistsException;
import com.rti.prueba.exception.ClienteNotFoundException;
import com.rti.prueba.exception.ClienteValidationException;
import com.rti.prueba.mapper.ClienteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de clientes
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ClienteService {

    private static final int EDAD_MINIMA_PRODUCTIVA = 18;
    private static final int EDAD_MAXIMA_PRODUCTIVA = 65;

    private final ClienteJPA clienteJPA;
    private final ClienteMapper clienteMapper;

    /**
     * Crear un nuevo cliente
     */
    public String crearCliente(ClienteCreateDTO clienteCreateDTO) {
        if (clienteJPA.existsById(clienteCreateDTO.getNumeroDocumento())) {
            throw new ClienteAlreadyExistsException(clienteCreateDTO.getNumeroDocumento());
        }

        Optional<ClienteORM> clienteExistente = clienteJPA.findByCorreoElectronico(clienteCreateDTO.getCorreoElectronico());
        if (clienteExistente.isPresent()) {
            throw new ClienteAlreadyExistsException("correo electrónico", clienteCreateDTO.getCorreoElectronico());
        }

        try {
            ClienteORM cliente = clienteMapper.createDTOToORM(clienteCreateDTO);
            cliente.setNumeroDocumento(clienteCreateDTO.getNumeroDocumento());
            cliente.setEsViable(esClienteViable(validarEdad(clienteCreateDTO.getFechaNacimiento())));

            clienteJPA.save(cliente);

            return "Cliente creado exitosamente. Es viable: " + (cliente.getEsViable() ? "Sí" : "No");

        } catch (DataIntegrityViolationException e) {
            throw new ClienteAlreadyExistsException("Ya existe un cliente con estos datos", e);
        }
    }

    /**
     * Obtener todos los clientes
     */
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> obtenerTodosLosClientes() {
        List<ClienteORM> clientes = clienteJPA.findAll();
        return clientes.stream()
                .map(this::mapearClienteConEdad)
                .collect(Collectors.toList());
    }

    /**
     * Obtener un cliente por número de documento
     */
    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerClientePorDocumento(String numeroDocumento) {
        ClienteORM cliente = clienteJPA.findById(numeroDocumento)
                .orElseThrow(() -> new ClienteNotFoundException(numeroDocumento));
        
        return mapearClienteConEdad(cliente);
    }

    /**
     * Actualizar un cliente
     */
    public String actualizarCliente(String numeroDocumento, ClienteUpdateDTO clienteUpdateDTO) {
        // Buscar cliente existente
        ClienteORM cliente = clienteJPA.findById(numeroDocumento)
                .orElseThrow(() -> new ClienteNotFoundException(numeroDocumento));

        // Validar que el correo no esté siendo usado por otro cliente
        Optional<ClienteORM> clienteConCorreo = clienteJPA.findByCorreoElectronico(clienteUpdateDTO.getCorreoElectronico());
        if (clienteConCorreo.isPresent() && !clienteConCorreo.get().getNumeroDocumento().equals(numeroDocumento)) {
            throw new ClienteAlreadyExistsException("correo electrónico", clienteUpdateDTO.getCorreoElectronico());
        }

        // Validar edad
        int edad = validarEdad(clienteUpdateDTO.getFechaNacimiento());

        try {
            // Actualizar datos
            clienteMapper.updateDTOToORM(clienteUpdateDTO, cliente);
            cliente.setEsViable(esClienteViable(edad));

            clienteJPA.save(cliente);

            return "Cliente actualizado exitosamente. Es viable: " + (cliente.getEsViable() ? "Sí" : "No");

        } catch (DataIntegrityViolationException e) {
            throw new ClienteAlreadyExistsException("Conflicto con datos existentes", e);
        }
    }

    /**
     * Eliminar un cliente
     */
    public String eliminarCliente(String numeroDocumento) {
        if (!clienteJPA.existsById(numeroDocumento)) {
            throw new ClienteNotFoundException(numeroDocumento);
        }

        clienteJPA.deleteById(numeroDocumento);
        return "Cliente eliminado exitosamente";
    }

    /**
     * Buscar clientes por nombre o apellidos
     */
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> buscarClientesPorNombreOApellidos(String busqueda) {
        List<ClienteORM> clientes = clienteJPA.findByNombreContainingIgnoreCaseOrApellidosContainingIgnoreCase(busqueda, busqueda);
        return clientes.stream()
                .map(this::mapearClienteConEdad)
                .collect(Collectors.toList());
    }

    /**
     * Calcular la edad de una persona
     */
    private int calcularEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento.isAfter(LocalDate.now())) {
            return -1; // Fecha futura
        }
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    /**
     * Determinar si un cliente es viable según su edad
     */
    private boolean esClienteViable(int edad) {
        return edad >= EDAD_MINIMA_PRODUCTIVA && edad <= EDAD_MAXIMA_PRODUCTIVA;
    }

    /**
     * Validar la edad y devolver el valor calculado
     */
    private int validarEdad(LocalDate fechaNacimiento) {
        int edad = calcularEdad(fechaNacimiento);
        if (edad < 0) {
            throw new ClienteValidationException("La fecha de nacimiento no puede ser futura");
        }
        return edad;
    }

    /**
     * Mapear cliente a DTO con edad calculada
     */
    private ClienteResponseDTO mapearClienteConEdad(ClienteORM cliente) {
        ClienteResponseDTO responseDTO = clienteMapper.ORMToResponseDTO(cliente);
        responseDTO.setEdad(calcularEdad(cliente.getFechaNacimiento()));
        return responseDTO;
    }
}

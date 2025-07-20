package com.rti.prueba.service;

import com.rti.prueba.bd.jpa.ClienteJPA;
import com.rti.prueba.bd.orm.ClienteORM;
import com.rti.prueba.controller.dto.ClienteCreateDTO;
import com.rti.prueba.controller.dto.ClienteResponseDTO;
import com.rti.prueba.controller.dto.ClienteUpdateDTO;
import com.rti.prueba.exception.ClienteAlreadyExistsException;
import com.rti.prueba.exception.ClienteNotFoundException;
import com.rti.prueba.exception.ClienteServiceException;
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
        try {
            // Validar que no exista un cliente con el mismo número de documento
            if (clienteJPA.existsById(clienteCreateDTO.getNumeroDocumento())) {
                throw new ClienteAlreadyExistsException(clienteCreateDTO.getNumeroDocumento());
            }

            // Validar que no exista un cliente con el mismo correo electrónico
            Optional<ClienteORM> clienteExistente = clienteJPA.findByCorreoElectronico(clienteCreateDTO.getCorreoElectronico());
            if (clienteExistente.isPresent()) {
                throw new ClienteAlreadyExistsException("correo electrónico", clienteCreateDTO.getCorreoElectronico());
            }

            // Validar edad mínima
            int edad = calcularEdad(clienteCreateDTO.getFechaNacimiento());
            if (edad < 0) {
                throw new ClienteValidationException("La fecha de nacimiento no puede ser futura");
            }

            // Crear la entidad usando el mapper
            ClienteORM cliente = clienteMapper.createDTOToORM(clienteCreateDTO);
            cliente.setNumeroDocumento(clienteCreateDTO.getNumeroDocumento());

            // Calcular viabilidad
            cliente.setEsViable(esClienteViable(edad));

            // Guardar en la base de datos
            clienteJPA.save(cliente);

            return "Cliente creado exitosamente. Es viable: " + (cliente.getEsViable() ? "Sí" : "No");

        } catch (DataIntegrityViolationException e) {
            throw new ClienteAlreadyExistsException("Ya existe un cliente con estos datos", e);
        } catch (Exception e) {
            throw new ClienteServiceException("Error al crear el cliente: " + e.getMessage(), e);
        }
    }

    /**
     * Obtener todos los clientes
     */
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> obtenerTodosLosClientes() {
        try {
            List<ClienteORM> clientes = clienteJPA.findAll();
            return clientes.stream()
                    .map(cliente -> {
                        ClienteResponseDTO responseDTO = clienteMapper.ORMToResponseDTO(cliente);
                        responseDTO.setEdad(calcularEdad(cliente.getFechaNacimiento()));
                        return responseDTO;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ClienteServiceException("Error al obtener los clientes: " + e.getMessage(), e);
        }
    }

    /**
     * Obtener un cliente por número de documento
     */
    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerClientePorDocumento(String numeroDocumento) {
        try {
            ClienteORM cliente = clienteJPA.findById(numeroDocumento)
                    .orElseThrow(() -> new ClienteNotFoundException(numeroDocumento));
            
            ClienteResponseDTO responseDTO = clienteMapper.ORMToResponseDTO(cliente);
            responseDTO.setEdad(calcularEdad(cliente.getFechaNacimiento()));
            return responseDTO;
        } catch (ClienteNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ClienteServiceException("Error al obtener el cliente: " + e.getMessage(), e);
        }
    }

    /**
     * Actualizar un cliente
     */
    public String actualizarCliente(String numeroDocumento, ClienteUpdateDTO clienteUpdateDTO) {
        try {
            ClienteORM cliente = clienteJPA.findById(numeroDocumento)
                    .orElseThrow(() -> new ClienteNotFoundException(numeroDocumento));

            // Validar que el correo no esté siendo usado por otro cliente
            Optional<ClienteORM> clienteConCorreo = clienteJPA.findByCorreoElectronico(clienteUpdateDTO.getCorreoElectronico());
            if (clienteConCorreo.isPresent() && !clienteConCorreo.get().getNumeroDocumento().equals(numeroDocumento)) {
                throw new ClienteAlreadyExistsException("correo electrónico", clienteUpdateDTO.getCorreoElectronico());
            }

            // Validar edad
            int edad = calcularEdad(clienteUpdateDTO.getFechaNacimiento());
            if (edad < 0) {
                throw new ClienteValidationException("La fecha de nacimiento no puede ser futura");
            }

            // Actualizar datos
            clienteMapper.updateDTOToORM(clienteUpdateDTO, cliente);

            // Recalcular viabilidad
            cliente.setEsViable(esClienteViable(edad));

            clienteJPA.save(cliente);

            return "Cliente actualizado exitosamente. Es viable: " + (cliente.getEsViable() ? "Sí" : "No");

        } catch (ClienteNotFoundException | ClienteAlreadyExistsException | ClienteValidationException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            throw new ClienteAlreadyExistsException("Conflicto con datos existentes", e);
        } catch (Exception e) {
            throw new ClienteServiceException("Error al actualizar el cliente: " + e.getMessage(), e);
        }
    }

    /**
     * Eliminar un cliente
     */
    public String eliminarCliente(String numeroDocumento) {
        try {
            if (!clienteJPA.existsById(numeroDocumento)) {
                throw new ClienteNotFoundException(numeroDocumento);
            }

            clienteJPA.deleteById(numeroDocumento);
            return "Cliente eliminado exitosamente";

        } catch (ClienteNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ClienteServiceException("Error al eliminar el cliente: " + e.getMessage(), e);
        }
    }

    /**
     * Buscar clientes por nombre o apellidos
     */
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> buscarClientesPorNombreOApellidos(String busqueda) {
        try {
            List<ClienteORM> clientes = clienteJPA.findByNombreContainingIgnoreCaseOrApellidosContainingIgnoreCase(busqueda, busqueda);
            return clientes.stream()
                    .map(cliente -> {
                        ClienteResponseDTO responseDTO = clienteMapper.ORMToResponseDTO(cliente);
                        responseDTO.setEdad(calcularEdad(cliente.getFechaNacimiento()));
                        return responseDTO;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ClienteServiceException("Error al buscar clientes: " + e.getMessage(), e);
        }
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
}

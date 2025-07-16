package com.rti.prueba.controller;

import com.rti.prueba.controller.dto.ClienteCreateDTO;
import com.rti.prueba.controller.dto.ClienteResponseDTO;
import com.rti.prueba.controller.dto.ClienteUpdateDTO;
import com.rti.prueba.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de clientes
 */
@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    /**
     * Crear un nuevo cliente
     * POST /api/clientes
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> crearCliente(@Valid @RequestBody ClienteCreateDTO clienteCreateDTO) {
        String mensaje = clienteService.crearCliente(clienteCreateDTO);
        
        Map<String, Object> response = Map.of(
                "success", true,
                "message", mensaje,
                "data", clienteCreateDTO.getNumeroDocumento()
        );
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Obtener todos los clientes
     * GET /api/clientes
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> obtenerTodosLosClientes() {
        List<ClienteResponseDTO> clientes = clienteService.obtenerTodosLosClientes();
        
        Map<String, Object> response = Map.of(
                "success", true,
                "message", "Clientes obtenidos exitosamente",
                "data", clientes,
                "total", clientes.size()
        );
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Obtener un cliente por número de documento
     * GET /api/clientes/{numeroDocumento}
     */
    @GetMapping("/{numeroDocumento}")
    public ResponseEntity<Map<String, Object>> obtenerClientePorDocumento(@PathVariable String numeroDocumento) {
        ClienteResponseDTO cliente = clienteService.obtenerClientePorDocumento(numeroDocumento);
        
        Map<String, Object> response = Map.of(
                "success", true,
                "message", "Cliente encontrado exitosamente",
                "data", cliente
        );
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Actualizar un cliente
     * PUT /api/clientes/{numeroDocumento}
     */
    @PutMapping("/{numeroDocumento}")
    public ResponseEntity<Map<String, Object>> actualizarCliente(
            @PathVariable String numeroDocumento,
            @Valid @RequestBody ClienteUpdateDTO clienteUpdateDTO) {
        
        String mensaje = clienteService.actualizarCliente(numeroDocumento, clienteUpdateDTO);
        
        Map<String, Object> response = Map.of(
                "success", true,
                "message", mensaje,
                "data", numeroDocumento
        );
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Eliminar un cliente
     * DELETE /api/clientes/{numeroDocumento}
     */
    @DeleteMapping("/{numeroDocumento}")
    public ResponseEntity<Map<String, Object>> eliminarCliente(@PathVariable String numeroDocumento) {
        String mensaje = clienteService.eliminarCliente(numeroDocumento);
        
        Map<String, Object> response = Map.of(
                "success", true,
                "message", mensaje,
                "data", numeroDocumento
        );
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Buscar clientes por nombre o apellidos
     * GET /api/clientes/buscar?q={termino}
     */
    @GetMapping("/buscar")
    public ResponseEntity<Map<String, Object>> buscarClientes(@RequestParam("q") String termino) {
        List<ClienteResponseDTO> clientes = clienteService.buscarClientesPorNombreOApellidos(termino);
        
        Map<String, Object> response = Map.of(
                "success", true,
                "message", "Búsqueda completada exitosamente",
                "data", clientes,
                "total", clientes.size(),
                "termino", termino
        );
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}

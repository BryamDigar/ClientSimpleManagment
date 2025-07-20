package com.rti.prueba.service;

import com.rti.prueba.bd.jpa.ClienteJPA;
import com.rti.prueba.bd.orm.ClienteORM;
import com.rti.prueba.controller.dto.ClienteCreateDTO;
import com.rti.prueba.controller.dto.ClienteResponseDTO;
import com.rti.prueba.controller.dto.ClienteUpdateDTO;
import com.rti.prueba.enums.Ocupacion;
import com.rti.prueba.exception.ClienteAlreadyExistsException;
import com.rti.prueba.exception.ClienteNotFoundException;
import com.rti.prueba.exception.ClienteValidationException;
import com.rti.prueba.mapper.ClienteMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService Tests")
class ClienteServiceTest {

    @Mock
    private ClienteJPA clienteJPA;
    
    @Mock
    private ClienteMapper clienteMapper;
    
    @InjectMocks
    private ClienteService clienteService;

    private ClienteCreateDTO clienteCreateDTO;
    private ClienteUpdateDTO clienteUpdateDTO;
    private ClienteORM clienteORM;
    private ClienteResponseDTO clienteResponseDTO;
    
    @BeforeEach
    void setUp() {
        // Configurar datos de prueba
        clienteCreateDTO = new ClienteCreateDTO(
                "12345678",
                "Juan Carlos",
                "Pérez González",
                LocalDate.of(1990, 5, 15), // 34 años - viable
                "Bogotá",
                "juan.perez@email.com",
                "3001234567",
                Ocupacion.EMPLEADO
        );
        
        clienteUpdateDTO = new ClienteUpdateDTO(
                "Juan Carlos Updated",
                "Pérez González Updated",
                LocalDate.of(1985, 3, 20), // 39 años - viable
                "Medellín",
                "juan.updated@email.com",
                "3007654321",
                Ocupacion.INDEPENDIENTE
        );
        
        clienteORM = new ClienteORM();
        clienteORM.setNumeroDocumento("12345678");
        clienteORM.setNombre("Juan Carlos");
        clienteORM.setApellidos("Pérez González");
        clienteORM.setFechaNacimiento(LocalDate.of(1990, 5, 15));
        clienteORM.setCiudad("Bogotá");
        clienteORM.setCorreoElectronico("juan.perez@email.com");
        clienteORM.setTelefono("3001234567");
        clienteORM.setOcupacion(Ocupacion.EMPLEADO);
        clienteORM.setEsViable(true);
        
        clienteResponseDTO = new ClienteResponseDTO(
                "12345678",
                "Juan Carlos",
                "Pérez González",
                LocalDate.of(1990, 5, 15),
                "Bogotá",
                "juan.perez@email.com",
                "3001234567",
                Ocupacion.EMPLEADO,
                true,
                34
        );
    }

    @Nested
    @DisplayName("Crear Cliente Tests")
    class CrearClienteTests {

        @Test
        @DisplayName("Given_clienteViable_When_crearCliente_Then_clienteCreadoExitosamente")
        void given_clienteViable_when_crearCliente_then_clienteCreadoExitosamente() {
            // Given
            when(clienteJPA.existsById(clienteCreateDTO.getNumeroDocumento())).thenReturn(false);
            when(clienteJPA.findByCorreoElectronico(clienteCreateDTO.getCorreoElectronico())).thenReturn(Optional.empty());
            when(clienteMapper.createDTOToORM(clienteCreateDTO)).thenReturn(clienteORM);
            when(clienteJPA.save(any(ClienteORM.class))).thenReturn(clienteORM);

            // When
            String resultado = clienteService.crearCliente(clienteCreateDTO);

            // Then
            assertThat(resultado).contains("Cliente creado exitosamente");
            assertThat(resultado).contains("Es viable: Sí");
            verify(clienteJPA).existsById(clienteCreateDTO.getNumeroDocumento());
            verify(clienteJPA).findByCorreoElectronico(clienteCreateDTO.getCorreoElectronico());
            verify(clienteMapper).createDTOToORM(clienteCreateDTO);
            verify(clienteJPA).save(any(ClienteORM.class));
        }

        @Test
        @DisplayName("Given_clienteNoViable_When_crearCliente_Then_clienteCreadoComoNoViable")
        void given_clienteNoViable_when_crearCliente_then_clienteCreadoComoNoViable() {
            // Given - Cliente menor de edad (17 años)
            ClienteCreateDTO clienteMenor = new ClienteCreateDTO(
                    "87654321",
                    "Ana María",
                    "López Silva",
                    LocalDate.of(2008, 7, 20), // 17 años exactos - no viable
                    "Cali",
                    "ana.lopez@email.com",
                    "3009876543",
                    Ocupacion.EMPLEADO
            );
            
            ClienteORM clienteNoViable = new ClienteORM();
            clienteNoViable.setNumeroDocumento("87654321");
            clienteNoViable.setEsViable(false); // Este valor será sobrescrito por el service
            
            when(clienteJPA.existsById(clienteMenor.getNumeroDocumento())).thenReturn(false);
            when(clienteJPA.findByCorreoElectronico(clienteMenor.getCorreoElectronico())).thenReturn(Optional.empty());
            when(clienteMapper.createDTOToORM(clienteMenor)).thenReturn(clienteNoViable);
            when(clienteJPA.save(any(ClienteORM.class))).thenReturn(clienteNoViable);

            // When
            String resultado = clienteService.crearCliente(clienteMenor);

            // Then
            assertThat(resultado).contains("Cliente creado exitosamente");
            assertThat(resultado).contains("Es viable: No");
            verify(clienteJPA).save(any(ClienteORM.class));
        }

        @Test
        @DisplayName("Given_clienteMayorNoViable_When_crearCliente_Then_clienteCreadoComoNoViable")
        void given_clienteMayorNoViable_when_crearCliente_then_clienteCreadoComoNoViable() {
            // Given - Cliente mayor de edad productiva (70 años)
            ClienteCreateDTO clienteMayor = new ClienteCreateDTO(
                    "99999999",
                    "Carlos Alberto",
                    "Rodríguez Martínez",
                    LocalDate.of(1955, 1, 1), // 70 años - no viable
                    "Medellín",
                    "carlos.rodriguez@email.com",
                    "3001111111",
                    Ocupacion.PENSIONADO
            );
            
            ClienteORM clienteNoViable = new ClienteORM();
            clienteNoViable.setNumeroDocumento("99999999");
            clienteNoViable.setEsViable(false);
            
            when(clienteJPA.existsById(clienteMayor.getNumeroDocumento())).thenReturn(false);
            when(clienteJPA.findByCorreoElectronico(clienteMayor.getCorreoElectronico())).thenReturn(Optional.empty());
            when(clienteMapper.createDTOToORM(clienteMayor)).thenReturn(clienteNoViable);
            when(clienteJPA.save(any(ClienteORM.class))).thenReturn(clienteNoViable);

            // When
            String resultado = clienteService.crearCliente(clienteMayor);

            // Then
            assertThat(resultado).contains("Cliente creado exitosamente");
            assertThat(resultado).contains("Es viable: No");
            verify(clienteJPA).save(any(ClienteORM.class));
        }

        @Test
        @DisplayName("Given_documentoExistente_When_crearCliente_Then_throwClienteAlreadyExistsException")
        void given_documentoExistente_when_crearCliente_then_throwClienteAlreadyExistsException() {
            // Given
            when(clienteJPA.existsById(clienteCreateDTO.getNumeroDocumento())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> clienteService.crearCliente(clienteCreateDTO))
                    .isInstanceOf(ClienteAlreadyExistsException.class)
                    .hasMessageContaining("Ya existe un cliente con el número de documento");

            verify(clienteJPA).existsById(clienteCreateDTO.getNumeroDocumento());
            verify(clienteJPA, never()).findByCorreoElectronico(anyString());
            verify(clienteJPA, never()).save(any());
        }

        @Test
        @DisplayName("Given_correoExistente_When_crearCliente_Then_throwClienteAlreadyExistsException")
        void given_correoExistente_when_crearCliente_then_throwClienteAlreadyExistsException() {
            // Given
            when(clienteJPA.existsById(clienteCreateDTO.getNumeroDocumento())).thenReturn(false);
            when(clienteJPA.findByCorreoElectronico(clienteCreateDTO.getCorreoElectronico())).thenReturn(Optional.of(clienteORM));

            // When & Then
            assertThatThrownBy(() -> clienteService.crearCliente(clienteCreateDTO))
                    .isInstanceOf(ClienteAlreadyExistsException.class)
                    .hasMessageContaining("Ya existe un cliente con correo electrónico");

            verify(clienteJPA).findByCorreoElectronico(clienteCreateDTO.getCorreoElectronico());
            verify(clienteJPA, never()).save(any());
        }

        @Test
        @DisplayName("Given_fechaNacimientoFutura_When_crearCliente_Then_throwClienteValidationException")
        void given_fechaNacimientoFutura_when_crearCliente_then_throwClienteValidationException() {
            // Given
            ClienteCreateDTO clienteFechaFutura = new ClienteCreateDTO(
                    "11111111",
                    "Future",
                    "Client",
                    LocalDate.of(2026, 1, 1), // Fecha futura
                    "Ciudad",
                    "future@email.com",
                    "3001111111",
                    Ocupacion.EMPLEADO
            );
            
            ClienteORM clienteTemp = new ClienteORM();
            clienteTemp.setNumeroDocumento("11111111");
            
            when(clienteJPA.existsById(clienteFechaFutura.getNumeroDocumento())).thenReturn(false);
            when(clienteJPA.findByCorreoElectronico(clienteFechaFutura.getCorreoElectronico())).thenReturn(Optional.empty());
            when(clienteMapper.createDTOToORM(clienteFechaFutura)).thenReturn(clienteTemp);

            // When & Then
            assertThatThrownBy(() -> clienteService.crearCliente(clienteFechaFutura))
                    .isInstanceOf(ClienteValidationException.class)
                    .hasMessageContaining("La fecha de nacimiento no puede ser futura");

            verify(clienteJPA).existsById(clienteFechaFutura.getNumeroDocumento());
            verify(clienteJPA).findByCorreoElectronico(clienteFechaFutura.getCorreoElectronico());
            verify(clienteMapper).createDTOToORM(clienteFechaFutura);
            verify(clienteJPA, never()).save(any());
        }

        @Test
        @DisplayName("Given_dataIntegrityViolation_When_crearCliente_Then_throwClienteAlreadyExistsException")
        void given_dataIntegrityViolation_when_crearCliente_then_throwClienteAlreadyExistsException() {
            // Given
            when(clienteJPA.existsById(clienteCreateDTO.getNumeroDocumento())).thenReturn(false);
            when(clienteJPA.findByCorreoElectronico(clienteCreateDTO.getCorreoElectronico())).thenReturn(Optional.empty());
            when(clienteMapper.createDTOToORM(clienteCreateDTO)).thenReturn(clienteORM);
            when(clienteJPA.save(any(ClienteORM.class))).thenThrow(new DataIntegrityViolationException("DB Constraint violation"));

            // When & Then
            assertThatThrownBy(() -> clienteService.crearCliente(clienteCreateDTO))
                    .isInstanceOf(ClienteAlreadyExistsException.class)
                    .hasMessageContaining("Ya existe un cliente con estos datos");

            verify(clienteJPA).save(any(ClienteORM.class));
        }
    }

    @Nested
    @DisplayName("Obtener Clientes Tests")
    class ObtenerClientesTests {

        @Test
        @DisplayName("Given_clientesExistentes_When_obtenerTodosLosClientes_Then_returnListaClientes")
        void given_clientesExistentes_when_obtenerTodosLosClientes_then_returnListaClientes() {
            // Given
            List<ClienteORM> clientes = Collections.singletonList(clienteORM);
            when(clienteJPA.findAll()).thenReturn(clientes);
            when(clienteMapper.ORMToResponseDTO(clienteORM)).thenReturn(clienteResponseDTO);

            // When
            List<ClienteResponseDTO> resultado = clienteService.obtenerTodosLosClientes();

            // Then
            assertThat(resultado).hasSize(1);
            assertThat(resultado.getFirst().getNumeroDocumento()).isEqualTo("12345678");
            assertThat(resultado.getFirst().getEdad()).isPositive();
            verify(clienteJPA).findAll();
            verify(clienteMapper).ORMToResponseDTO(clienteORM);
        }

        @Test
        @DisplayName("Given_noClientesExistentes_When_obtenerTodosLosClientes_Then_returnListaVacia")
        void given_noClientesExistentes_when_obtenerTodosLosClientes_then_returnListaVacia() {
            // Given
            when(clienteJPA.findAll()).thenReturn(Collections.emptyList());

            // When
            List<ClienteResponseDTO> resultado = clienteService.obtenerTodosLosClientes();

            // Then
            assertThat(resultado).isEmpty();
            verify(clienteJPA).findAll();
            verify(clienteMapper, never()).ORMToResponseDTO(any());
        }

        @Test
        @DisplayName("Given_clienteExistente_When_obtenerClientePorDocumento_Then_returnCliente")
        void given_clienteExistente_when_obtenerClientePorDocumento_then_returnCliente() {
            // Given
            String numeroDocumento = "12345678";
            when(clienteJPA.findById(numeroDocumento)).thenReturn(Optional.of(clienteORM));
            when(clienteMapper.ORMToResponseDTO(clienteORM)).thenReturn(clienteResponseDTO);

            // When
            ClienteResponseDTO resultado = clienteService.obtenerClientePorDocumento(numeroDocumento);

            // Then
            assertThat(resultado).isNotNull();
            assertThat(resultado.getNumeroDocumento()).isEqualTo(numeroDocumento);
            assertThat(resultado.getEdad()).isPositive();
            verify(clienteJPA).findById(numeroDocumento);
            verify(clienteMapper).ORMToResponseDTO(clienteORM);
        }

        @Test
        @DisplayName("Given_clienteNoExistente_When_obtenerClientePorDocumento_Then_throwClienteNotFoundException")
        void given_clienteNoExistente_when_obtenerClientePorDocumento_then_throwClienteNotFoundException() {
            // Given
            String numeroDocumento = "99999999";
            when(clienteJPA.findById(numeroDocumento)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> clienteService.obtenerClientePorDocumento(numeroDocumento))
                    .isInstanceOf(ClienteNotFoundException.class)
                    .hasMessageContaining("Cliente con número de documento '99999999' no encontrado");

            verify(clienteJPA).findById(numeroDocumento);
            verify(clienteMapper, never()).ORMToResponseDTO(any());
        }
    }

    @Nested
    @DisplayName("Actualizar Cliente Tests")
    class ActualizarClienteTests {

        @Test
        @DisplayName("Given_clienteValidoExistente_When_actualizarCliente_Then_clienteActualizadoExitosamente")
        void given_clienteValidoExistente_when_actualizarCliente_then_clienteActualizadoExitosamente() {
            // Given
            String numeroDocumento = "12345678";
            when(clienteJPA.findById(numeroDocumento)).thenReturn(Optional.of(clienteORM));
            when(clienteJPA.findByCorreoElectronico(clienteUpdateDTO.getCorreoElectronico())).thenReturn(Optional.empty());
            when(clienteJPA.save(any(ClienteORM.class))).thenReturn(clienteORM);

            // When
            String resultado = clienteService.actualizarCliente(numeroDocumento, clienteUpdateDTO);

            // Then
            assertThat(resultado).contains("Cliente actualizado exitosamente");
            assertThat(resultado).contains("Es viable: Sí");
            verify(clienteJPA).findById(numeroDocumento);
            verify(clienteJPA).findByCorreoElectronico(clienteUpdateDTO.getCorreoElectronico());
            verify(clienteMapper).updateDTOToORM(clienteUpdateDTO, clienteORM);
            verify(clienteJPA).save(clienteORM);
        }

        @Test
        @DisplayName("Given_clienteNoExistente_When_actualizarCliente_Then_throwClienteNotFoundException")
        void given_clienteNoExistente_when_actualizarCliente_then_throwClienteNotFoundException() {
            // Given
            String numeroDocumento = "99999999";
            when(clienteJPA.findById(numeroDocumento)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> clienteService.actualizarCliente(numeroDocumento, clienteUpdateDTO))
                    .isInstanceOf(ClienteNotFoundException.class)
                    .hasMessageContaining("Cliente con número de documento '99999999' no encontrado");

            verify(clienteJPA).findById(numeroDocumento);
            verify(clienteJPA, never()).save(any());
        }

        @Test
        @DisplayName("Given_correoEnUsoOtroCliente_When_actualizarCliente_Then_throwClienteAlreadyExistsException")
        void given_correoEnUsoOtroCliente_when_actualizarCliente_then_throwClienteAlreadyExistsException() {
            // Given
            String numeroDocumento = "12345678";
            ClienteORM otroCliente = new ClienteORM();
            otroCliente.setNumeroDocumento("87654321"); // Diferente documento
            
            when(clienteJPA.findById(numeroDocumento)).thenReturn(Optional.of(clienteORM));
            when(clienteJPA.findByCorreoElectronico(clienteUpdateDTO.getCorreoElectronico())).thenReturn(Optional.of(otroCliente));

            // When & Then
            assertThatThrownBy(() -> clienteService.actualizarCliente(numeroDocumento, clienteUpdateDTO))
                    .isInstanceOf(ClienteAlreadyExistsException.class)
                    .hasMessageContaining("Ya existe un cliente con correo electrónico");

            verify(clienteJPA).findById(numeroDocumento);
            verify(clienteJPA).findByCorreoElectronico(clienteUpdateDTO.getCorreoElectronico());
            verify(clienteJPA, never()).save(any());
        }

        @Test
        @DisplayName("Given_fechaNacimientoFutura_When_actualizarCliente_Then_throwClienteValidationException")
        void given_fechaNacimientoFutura_when_actualizarCliente_then_throwClienteValidationException() {
            // Given
            String numeroDocumento = "12345678";
            ClienteUpdateDTO updateConFechaFutura = new ClienteUpdateDTO(
                    "Nombre",
                    "Apellido",
                    LocalDate.now().plusDays(1), // Fecha futura
                    "Ciudad",
                    "correo@email.com",
                    "3001111111",
                    Ocupacion.EMPLEADO
            );
            
            when(clienteJPA.findById(numeroDocumento)).thenReturn(Optional.of(clienteORM));
            when(clienteJPA.findByCorreoElectronico(updateConFechaFutura.getCorreoElectronico())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> clienteService.actualizarCliente(numeroDocumento, updateConFechaFutura))
                    .isInstanceOf(ClienteValidationException.class)
                    .hasMessageContaining("La fecha de nacimiento no puede ser futura");

            verify(clienteJPA, never()).save(any());
        }

        @Test
        @DisplayName("Given_dataIntegrityViolation_When_actualizarCliente_Then_throwClienteAlreadyExistsException")
        void given_dataIntegrityViolation_when_actualizarCliente_then_throwClienteAlreadyExistsException() {
            // Given
            String numeroDocumento = "12345678";
            when(clienteJPA.findById(numeroDocumento)).thenReturn(Optional.of(clienteORM));
            when(clienteJPA.findByCorreoElectronico(clienteUpdateDTO.getCorreoElectronico())).thenReturn(Optional.empty());
            when(clienteJPA.save(any(ClienteORM.class))).thenThrow(new DataIntegrityViolationException("DB Constraint violation"));

            // When & Then
            assertThatThrownBy(() -> clienteService.actualizarCliente(numeroDocumento, clienteUpdateDTO))
                    .isInstanceOf(ClienteAlreadyExistsException.class)
                    .hasMessageContaining("Conflicto con datos existentes");

            verify(clienteJPA).save(any(ClienteORM.class));
        }
    }

    @Nested
    @DisplayName("Eliminar Cliente Tests")
    class EliminarClienteTests {

        @Test
        @DisplayName("Given_clienteExistente_When_eliminarCliente_Then_clienteEliminadoExitosamente")
        void given_clienteExistente_when_eliminarCliente_then_clienteEliminadoExitosamente() {
            // Given
            String numeroDocumento = "12345678";
            when(clienteJPA.existsById(numeroDocumento)).thenReturn(true);

            // When
            String resultado = clienteService.eliminarCliente(numeroDocumento);

            // Then
            assertThat(resultado).isEqualTo("Cliente eliminado exitosamente");
            verify(clienteJPA).existsById(numeroDocumento);
            verify(clienteJPA).deleteById(numeroDocumento);
        }

        @Test
        @DisplayName("Given_clienteNoExistente_When_eliminarCliente_Then_throwClienteNotFoundException")
        void given_clienteNoExistente_when_eliminarCliente_then_throwClienteNotFoundException() {
            // Given
            String numeroDocumento = "99999999";
            when(clienteJPA.existsById(numeroDocumento)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> clienteService.eliminarCliente(numeroDocumento))
                    .isInstanceOf(ClienteNotFoundException.class)
                    .hasMessageContaining("Cliente con número de documento '99999999' no encontrado");

            verify(clienteJPA).existsById(numeroDocumento);
            verify(clienteJPA, never()).deleteById(anyString());
        }
    }

    @Nested
    @DisplayName("Buscar Clientes Tests")
    class BuscarClientesTests {

        @Test
        @DisplayName("Given_clientesCoincidentes_When_buscarClientesPorNombreOApellidos_Then_returnClientesEncontrados")
        void given_clientesCoincidentes_when_buscarClientesPorNombreOApellidos_then_returnClientesEncontrados() {
            // Given
            String busqueda = "Juan";
            List<ClienteORM> clientes = Collections.singletonList(clienteORM);
            when(clienteJPA.findByNombreContainingIgnoreCaseOrApellidosContainingIgnoreCase(busqueda, busqueda)).thenReturn(clientes);
            when(clienteMapper.ORMToResponseDTO(clienteORM)).thenReturn(clienteResponseDTO);

            // When
            List<ClienteResponseDTO> resultado = clienteService.buscarClientesPorNombreOApellidos(busqueda);

            // Then
            assertThat(resultado).hasSize(1);
            assertThat(resultado.getFirst().getNombre()).contains("Juan");
            verify(clienteJPA).findByNombreContainingIgnoreCaseOrApellidosContainingIgnoreCase(busqueda, busqueda);
            verify(clienteMapper).ORMToResponseDTO(clienteORM);
        }

        @Test
        @DisplayName("Given_noClientesCoincidentes_When_buscarClientesPorNombreOApellidos_Then_returnListaVacia")
        void given_noClientesCoincidentes_when_buscarClientesPorNombreOApellidos_then_returnListaVacia() {
            // Given
            String busqueda = "NoExiste";
            when(clienteJPA.findByNombreContainingIgnoreCaseOrApellidosContainingIgnoreCase(busqueda, busqueda)).thenReturn(Collections.emptyList());

            // When
            List<ClienteResponseDTO> resultado = clienteService.buscarClientesPorNombreOApellidos(busqueda);

            // Then
            assertThat(resultado).isEmpty();
            verify(clienteJPA).findByNombreContainingIgnoreCaseOrApellidosContainingIgnoreCase(busqueda, busqueda);
            verify(clienteMapper, never()).ORMToResponseDTO(any());
        }
    }
}
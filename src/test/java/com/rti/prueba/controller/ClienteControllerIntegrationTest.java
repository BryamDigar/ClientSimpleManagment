package com.rti.prueba.controller;

import com.rti.prueba.bd.jpa.ClienteJPA;
import com.rti.prueba.controller.dto.ClienteCreateDTO;
import com.rti.prueba.controller.dto.ClienteUpdateDTO;
import com.rti.prueba.enums.Ocupacion;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests de integración completos para ClienteController
 * Prueba desde HTTP Request hasta HTTP Response
 * Incluye toda la stack: Controller → Service → JPA → H2 Database
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Cliente Controller Integration Tests")
class ClienteControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ClienteJPA clienteJPA;

    private String baseUrl;
    private ClienteCreateDTO clienteCreateDTO;
    private ClienteUpdateDTO clienteUpdateDTO;
    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/clientes";
        
        // Limpiar base de datos antes de cada test
        clienteJPA.deleteAll();
        
        // Configurar headers
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Datos de prueba
        clienteCreateDTO = new ClienteCreateDTO(
                "12345678",
                "Juan Carlos",
                "Pérez González",
                LocalDate.of(1990, 5, 15), // 35 años - viable
                "Bogotá",
                "juan.perez@email.com",
                "3001234567",
                Ocupacion.EMPLEADO
        );
        
        clienteUpdateDTO = new ClienteUpdateDTO(
                "Juan Carlos Updated",
                "Pérez González Updated",
                LocalDate.of(1985, 3, 20), // 40 años - viable
                "Medellín",
                "juan.updated@email.com",
                "3007654321",
                Ocupacion.INDEPENDIENTE
        );
    }

    // Helper methods para evitar duplicación de código
    private HttpEntity<ClienteCreateDTO> createHttpRequest(ClienteCreateDTO dto) {
        return new HttpEntity<>(dto, headers);
    }

    private ResponseEntity<String> postCliente(ClienteCreateDTO dto) {
        return restTemplate.exchange(baseUrl, HttpMethod.POST, createHttpRequest(dto), String.class);
    }

    private void assertClienteExistsInDatabase(String numeroDocumento) {
        assertThat(clienteJPA.existsById(numeroDocumento))
                .as("Cliente con documento %s debería existir en la base de datos", numeroDocumento)
                .isTrue();
    }

    @Nested
    @DisplayName("POST /api/clientes - Crear Cliente")
    class CrearClienteIntegrationTests {

        @Test
        @Order(1)
        @DisplayName("Given_clienteValido_When_POST_Then_201CreatedWithSuccessResponse")
        void given_clienteValido_when_post_then_201CreatedWithSuccessResponse() {
            // Given
            // When
            ResponseEntity<String> response = postCliente(clienteCreateDTO);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).contains("\"success\":true");
            assertThat(response.getBody()).contains("Cliente creado exitosamente");
            assertThat(response.getBody()).contains("Es viable: Sí");
            assertThat(response.getBody()).contains("\"data\":\"12345678\"");
            
            // Verificar persistencia en BD
            assertClienteExistsInDatabase("12345678");
        }

        @Test
        @Order(2)
        @DisplayName("Given_clienteNoViableMenor_When_POST_Then_201CreatedButNotViable")
        void given_clienteNoViableMenor_when_post_then_201CreatedButNotViable() {
            // Given - Cliente menor de edad
            ClienteCreateDTO clienteMenor = new ClienteCreateDTO(
                    "87654321", "Ana María", "López Silva",
                    LocalDate.of(2008, 7, 20), // 17 años
                    "Cali", "ana@email.com", "3009876543", Ocupacion.EMPLEADO
            );

            // When
            ResponseEntity<String> response = postCliente(clienteMenor);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).contains("\"success\":true");
            assertThat(response.getBody()).contains("Es viable: No");
            assertThat(response.getBody()).contains("\"data\":\"87654321\"");
            
            // Verificar persistencia con viabilidad falsa
            assertClienteExistsInDatabase("87654321");
            clienteJPA.findById("87654321").ifPresentOrElse(
                cliente -> assertThat(cliente.getEsViable()).isFalse(),
                () -> fail("Cliente debería existir en la base de datos")
            );
        }

        @Test
        @Order(3)
        @DisplayName("Given_clienteNoViableMayor_When_POST_Then_201CreatedButNotViable")
        void given_clienteNoViableMayor_when_post_then_201CreatedButNotViable() {
            // Given - Cliente mayor de edad productiva
            ClienteCreateDTO clienteMayor = new ClienteCreateDTO(
                    "11111111", "Carlos Alberto", "Rodríguez Martínez",
                    LocalDate.of(1955, 1, 1), // 70 años
                    "Medellín", "carlos.rodriguez@email.com", "3001111111", Ocupacion.PENSIONADO
            );
            HttpEntity<ClienteCreateDTO> request = new HttpEntity<>(clienteMayor, headers);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl, HttpMethod.POST, request, String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).contains("\"success\":true");
            assertThat(response.getBody()).contains("Es viable: No");
            clienteJPA.findById("11111111").ifPresentOrElse(
                cliente -> assertThat(cliente.getEsViable()).isFalse(),
                () -> fail("Cliente debería existir en la base de datos")
            );
        }

        @Test
        @Order(4)
        @DisplayName("Given_documentoDuplicado_When_POST_Then_409ConflictError")
        void given_documentoDuplicado_when_post_then_409ConflictError() {
            // Given - Crear primer cliente
            postCliente(clienteCreateDTO);
            
            // Intentar crear cliente con mismo documento
            ClienteCreateDTO clienteDuplicado = new ClienteCreateDTO(
                    "12345678", "Otro", "Cliente", LocalDate.of(1992, 1, 1),
                    "Otra Ciudad", "otro@email.com", "3001111111", Ocupacion.INDEPENDIENTE
            );

            // When
            ResponseEntity<String> response = postCliente(clienteDuplicado);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).contains("\"error\":\"Cliente ya existe\"");
            assertThat(response.getBody()).contains("Ya existe un cliente con el número de documento");
        }

        @Test
        @Order(5)
        @DisplayName("Given_correoDuplicado_When_POST_Then_409ConflictError")
        void given_correoDuplicado_when_post_then_409ConflictError() {
            // Given - Crear primer cliente
            HttpEntity<ClienteCreateDTO> request1 = new HttpEntity<>(clienteCreateDTO, headers);
            restTemplate.exchange(baseUrl, HttpMethod.POST, request1, String.class);
            
            // Intentar crear cliente con mismo correo
            ClienteCreateDTO clienteCorreoDuplicado = new ClienteCreateDTO(
                    "99999999", "Otro Nombre", "Otros Apellidos",
                    LocalDate.of(1992, 1, 1), "Otra Ciudad",
                    "juan.perez@email.com", // Mismo correo
                    "3001111111", Ocupacion.INDEPENDIENTE
            );
            HttpEntity<ClienteCreateDTO> request2 = new HttpEntity<>(clienteCorreoDuplicado, headers);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl, HttpMethod.POST, request2, String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).contains("Ya existe un cliente con correo electrónico");
        }

        @Test
        @Order(6)
        @DisplayName("Given_fechaFutura_When_POST_Then_400BadRequestValidationError")
        void given_fechaFutura_when_post_then_400BadRequestValidationError() {
            // Given
            ClienteCreateDTO clienteFechaFutura = new ClienteCreateDTO(
                    "22222222", "Future", "Client", LocalDate.of(2026, 1, 1),
                    "Ciudad", "future@email.com", "3001111111", Ocupacion.EMPLEADO
            );
            HttpEntity<ClienteCreateDTO> request = new HttpEntity<>(clienteFechaFutura, headers);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl, HttpMethod.POST, request, String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).contains("\"error\":\"Error de validación en los datos enviados\"");
            assertThat(response.getBody()).contains("La fecha de nacimiento debe ser anterior a la fecha actual");
        }

        @Test
        @Order(7)
        @DisplayName("Given_datosInvalidosValidation_When_POST_Then_400BadRequestWithFieldErrors")
        void given_datosInvalidosValidation_when_post_then_400BadRequestWithFieldErrors() {
            // Given - Cliente con datos que fallan validación de Bean Validation
            ClienteCreateDTO clienteInvalido = new ClienteCreateDTO(
                    "", // Documento vacío - @NotBlank
                    "", // Nombre vacío - @NotBlank
                    "Apellidos",
                    LocalDate.of(1992, 1, 1),
                    "Ciudad",
                    "email-invalido", // Email inválido - @Email
                    "telefono-invalido", // Teléfono inválido - @Pattern
                    Ocupacion.EMPLEADO
            );
            HttpEntity<ClienteCreateDTO> request = new HttpEntity<>(clienteInvalido, headers);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl, HttpMethod.POST, request, String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).contains("\"error\":\"Error de validación en los datos enviados\"");
            // Debería contener errores de campos específicos
            assertThat(response.getBody()).contains("fieldErrors");
        }
    }

    @Nested
    @DisplayName("GET /api/clientes - Obtener Clientes")
    class ObtenerClientesIntegrationTests {

        @Test
        @Order(10)
        @DisplayName("Given_clientesEnBD_When_GET_Then_200OkWithClientesList")
        void given_clientesEnBD_when_get_then_200OkWithClientesList() {
            // Given - Crear varios clientes
            HttpEntity<ClienteCreateDTO> request1 = new HttpEntity<>(clienteCreateDTO, headers);
            restTemplate.exchange(baseUrl, HttpMethod.POST, request1, String.class);
            
            ClienteCreateDTO cliente2 = new ClienteCreateDTO(
                    "87654321", "María José", "García López",
                    LocalDate.of(1995, 6, 10), "Cali", "maria@email.com",
                    "3009876543", Ocupacion.INDEPENDIENTE
            );
            HttpEntity<ClienteCreateDTO> request2 = new HttpEntity<>(cliente2, headers);
            restTemplate.exchange(baseUrl, HttpMethod.POST, request2, String.class);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl, HttpMethod.GET, null, String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"success\":true");
            assertThat(response.getBody()).contains("Clientes obtenidos exitosamente");
            assertThat(response.getBody()).contains("\"total\":2");
            assertThat(response.getBody()).contains("Juan Carlos");
            assertThat(response.getBody()).contains("María José");
            // Verificar que incluye edad calculada
            assertThat(response.getBody()).contains("\"edad\":");
        }

        @Test
        @Order(11)
        @DisplayName("Given_baseDatosVacia_When_GET_Then_200OkWithEmptyList")
        void given_baseDatosVacia_when_get_then_200OkWithEmptyList() {
            // Given - BD vacía (se limpia en setUp)

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl, HttpMethod.GET, null, String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"success\":true");
            assertThat(response.getBody()).contains("\"total\":0");
            assertThat(response.getBody()).contains("\"data\":[]");
        }

        @Test
        @Order(12)
        @DisplayName("Given_clienteExistente_When_GET_byId_Then_200OkWithClienteData")
        void given_clienteExistente_when_getById_then_200OkWithClienteData() {
            // Given
            HttpEntity<ClienteCreateDTO> request = new HttpEntity<>(clienteCreateDTO, headers);
            restTemplate.exchange(baseUrl, HttpMethod.POST, request, String.class);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/12345678", HttpMethod.GET, null, String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"success\":true");
            assertThat(response.getBody()).contains("Cliente encontrado exitosamente");
            assertThat(response.getBody()).contains("\"numeroDocumento\":\"12345678\"");
            assertThat(response.getBody()).contains("\"nombre\":\"Juan Carlos\"");
            assertThat(response.getBody()).contains("\"esViable\":true");
            // Verificar que incluye edad calculada
            assertThat(response.getBody()).contains("\"edad\":");
        }

        @Test
        @Order(13)
        @DisplayName("Given_clienteNoExistente_When_GET_byId_Then_404NotFoundError")
        void given_clienteNoExistente_when_getById_then_404NotFoundError() {
            // Given - BD vacía

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/99999999", HttpMethod.GET, null, String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).contains("\"error\":\"Cliente no encontrado\"");
            assertThat(response.getBody()).contains("Cliente con número de documento '99999999' no encontrado");
        }
    }

    @Nested
    @DisplayName("PUT /api/clientes/{id} - Actualizar Cliente")
    class ActualizarClienteIntegrationTests {

        @Test
        @Order(20)
        @DisplayName("Given_clienteExistente_When_PUT_Then_200OkWithUpdateSuccess")
        void given_clienteExistente_when_put_then_200OkWithUpdateSuccess() {
            // Given - Crear cliente primero
            HttpEntity<ClienteCreateDTO> createRequest = new HttpEntity<>(clienteCreateDTO, headers);
            restTemplate.exchange(baseUrl, HttpMethod.POST, createRequest, String.class);
            
            HttpEntity<ClienteUpdateDTO> updateRequest = new HttpEntity<>(clienteUpdateDTO, headers);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/12345678", HttpMethod.PUT, updateRequest, String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"success\":true");
            assertThat(response.getBody()).contains("Cliente actualizado exitosamente");
            assertThat(response.getBody()).contains("\"data\":\"12345678\"");
            
            // Verificar que los datos se actualizaron en BD
            ResponseEntity<String> getResponse = restTemplate.exchange(
                    baseUrl + "/12345678", HttpMethod.GET, null, String.class
            );
            assertThat(getResponse.getBody()).contains("\"nombre\":\"Juan Carlos Updated\"");
            assertThat(getResponse.getBody()).contains("\"ciudad\":\"Medellín\"");
            assertThat(getResponse.getBody()).contains("\"correoElectronico\":\"juan.updated@email.com\"");
        }

        @Test
        @Order(21)
        @DisplayName("Given_clienteNoExistente_When_PUT_Then_404NotFoundError")
        void given_clienteNoExistente_when_put_then_404NotFoundError() {
            // Given - BD vacía
            HttpEntity<ClienteUpdateDTO> request = new HttpEntity<>(clienteUpdateDTO, headers);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/99999999", HttpMethod.PUT, request, String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).contains("\"error\":\"Cliente no encontrado\"");
        }

        @Test
        @Order(22)
        @DisplayName("Given_correoEnUso_When_PUT_Then_409ConflictError")
        void given_correoEnUso_when_put_then_409ConflictError() {
            // Given - Crear dos clientes
            HttpEntity<ClienteCreateDTO> request1 = new HttpEntity<>(clienteCreateDTO, headers);
            restTemplate.exchange(baseUrl, HttpMethod.POST, request1, String.class);
            
            ClienteCreateDTO cliente2 = new ClienteCreateDTO(
                    "87654321", "María", "García", LocalDate.of(1992, 1, 1),
                    "Cali", "maria@email.com", "3009876543", Ocupacion.INDEPENDIENTE
            );
            HttpEntity<ClienteCreateDTO> request2 = new HttpEntity<>(cliente2, headers);
            restTemplate.exchange(baseUrl, HttpMethod.POST, request2, String.class);
            
            // Intentar actualizar cliente2 con el correo de cliente1
            ClienteUpdateDTO updateConCorreoExistente = new ClienteUpdateDTO(
                    "María Updated", "García Updated", LocalDate.of(1992, 1, 1),
                    "Cali", "juan.perez@email.com", // Correo ya usado por cliente1
                    "3009876543", Ocupacion.INDEPENDIENTE
            );
            HttpEntity<ClienteUpdateDTO> updateRequest = new HttpEntity<>(updateConCorreoExistente, headers);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/87654321", HttpMethod.PUT, updateRequest, String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).contains("Ya existe un cliente con correo electrónico");
        }
    }

    @Nested
    @DisplayName("DELETE /api/clientes/{id} - Eliminar Cliente")
    class EliminarClienteIntegrationTests {

        @Test
        @Order(30)
        @DisplayName("Given_clienteExistente_When_DELETE_Then_200OkWithDeleteSuccess")
        void given_clienteExistente_when_delete_then_200OkWithDeleteSuccess() {
            // Given
            HttpEntity<ClienteCreateDTO> createRequest = new HttpEntity<>(clienteCreateDTO, headers);
            restTemplate.exchange(baseUrl, HttpMethod.POST, createRequest, String.class);
            assertThat(clienteJPA.existsById("12345678")).isTrue();

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/12345678", HttpMethod.DELETE, null, String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"success\":true");
            assertThat(response.getBody()).contains("Cliente eliminado exitosamente");
            assertThat(response.getBody()).contains("\"data\":\"12345678\"");
            
            // Verificar que se eliminó de la BD
            assertThat(clienteJPA.existsById("12345678")).isFalse();
        }

        @Test
        @Order(31)
        @DisplayName("Given_clienteNoExistente_When_DELETE_Then_404NotFoundError")
        void given_clienteNoExistente_when_delete_then_404NotFoundError() {
            // Given - BD vacía

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/99999999", HttpMethod.DELETE, null, String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).contains("\"error\":\"Cliente no encontrado\"");
            assertThat(response.getBody()).contains("Cliente con número de documento '99999999' no encontrado");
        }
    }

    @Nested
    @DisplayName("GET /api/clientes/buscar?q={term} - Buscar Clientes")
    class BuscarClientesIntegrationTests {

        @Test
        @Order(40)
        @DisplayName("Given_clientesCoincidentes_When_GET_search_Then_200OkWithFilteredResults")
        void given_clientesCoincidentes_when_getSearch_then_200OkWithFilteredResults() {
            // Given - Crear varios clientes
            HttpEntity<ClienteCreateDTO> request1 = new HttpEntity<>(clienteCreateDTO, headers); // Juan Carlos
            restTemplate.exchange(baseUrl, HttpMethod.POST, request1, String.class);
            
            ClienteCreateDTO cliente2 = new ClienteCreateDTO(
                    "87654321", "Juan Pablo", "Martínez Silva",
                    LocalDate.of(1995, 6, 10), "Cali", "juanp@email.com",
                    "3009876543", Ocupacion.INDEPENDIENTE
            );
            HttpEntity<ClienteCreateDTO> request2 = new HttpEntity<>(cliente2, headers);
            restTemplate.exchange(baseUrl, HttpMethod.POST, request2, String.class);

            ClienteCreateDTO cliente3 = new ClienteCreateDTO(
                    "11111111", "María José", "García López",
                    LocalDate.of(1993, 3, 15), "Medellín", "maria@email.com",
                    "3005555555", Ocupacion.EMPLEADO
            );
            HttpEntity<ClienteCreateDTO> request3 = new HttpEntity<>(cliente3, headers);
            restTemplate.exchange(baseUrl, HttpMethod.POST, request3, String.class);

            // When - Buscar por "Juan"
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/buscar?q=Juan", HttpMethod.GET, null, String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"success\":true");
            assertThat(response.getBody()).contains("Búsqueda completada exitosamente");
            assertThat(response.getBody()).contains("\"total\":2");
            assertThat(response.getBody()).contains("\"termino\":\"Juan\"");
            assertThat(response.getBody()).contains("Juan Carlos");
            assertThat(response.getBody()).contains("Juan Pablo");
            assertThat(response.getBody()).doesNotContain("María José");
        }

        @Test
        @Order(41)
        @DisplayName("Given_busquedaSinResultados_When_GET_search_Then_200OkWithEmptyResults")
        void given_busquedaSinResultados_when_getSearch_then_200OkWithEmptyResults() {
            // Given
            HttpEntity<ClienteCreateDTO> request = new HttpEntity<>(clienteCreateDTO, headers);
            restTemplate.exchange(baseUrl, HttpMethod.POST, request, String.class);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/buscar?q=NoExiste", HttpMethod.GET, null, String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"success\":true");
            assertThat(response.getBody()).contains("\"total\":0");
            assertThat(response.getBody()).contains("\"data\":[]");
            assertThat(response.getBody()).contains("\"termino\":\"NoExiste\"");
        }

        @Test
        @Order(42)
        @DisplayName("Given_busquedaPorApellidos_When_GET_search_Then_200OkWithMatchingResults")
        void given_busquedaPorApellidos_when_getSearch_then_200OkWithMatchingResults() {
            // Given
            HttpEntity<ClienteCreateDTO> request = new HttpEntity<>(clienteCreateDTO, headers); // Pérez González
            restTemplate.exchange(baseUrl, HttpMethod.POST, request, String.class);

            // When - Buscar por apellido
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/buscar?q=Pérez", HttpMethod.GET, null, String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"total\":1");
            assertThat(response.getBody()).contains("Juan Carlos");
            assertThat(response.getBody()).contains("Pérez González");
        }
    }

    @Nested
    @DisplayName("End-to-End Business Rules Tests")
    class EndToEndBusinessRulesTests {

        @Test
        @Order(50)
        @DisplayName("Given_clienteCompleto_When_fullCRUDFlow_Then_allOperationsWorkCorrectly")
        void given_clienteCompleto_when_fullCRUDFlow_then_allOperationsWorkCorrectly() {
            // CREATE
            HttpEntity<ClienteCreateDTO> createRequest = new HttpEntity<>(clienteCreateDTO, headers);
            ResponseEntity<String> createResponse = restTemplate.exchange(
                    baseUrl, HttpMethod.POST, createRequest, String.class
            );
            assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(createResponse.getBody()).contains("Es viable: Sí");

            // READ
            ResponseEntity<String> readResponse = restTemplate.exchange(
                    baseUrl + "/12345678", HttpMethod.GET, null, String.class
            );
            assertThat(readResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(readResponse.getBody()).contains("\"esViable\":true");

            // UPDATE
            HttpEntity<ClienteUpdateDTO> updateRequest = new HttpEntity<>(clienteUpdateDTO, headers);
            ResponseEntity<String> updateResponse = restTemplate.exchange(
                    baseUrl + "/12345678", HttpMethod.PUT, updateRequest, String.class
            );
            assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            // SEARCH
            ResponseEntity<String> searchResponse = restTemplate.exchange(
                    baseUrl + "/buscar?q=Juan", HttpMethod.GET, null, String.class
            );
            assertThat(searchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(searchResponse.getBody()).contains("\"total\":1");

            // DELETE
            ResponseEntity<String> deleteResponse = restTemplate.exchange(
                    baseUrl + "/12345678", HttpMethod.DELETE, null, String.class
            );
            assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            // VERIFY DELETE
            ResponseEntity<String> verifyResponse = restTemplate.exchange(
                    baseUrl + "/12345678", HttpMethod.GET, null, String.class
            );
            assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @Order(51)
        @DisplayName("Given_clientesVariasEdades_When_crearVarios_Then_viabilidadCalculadaCorrectamente")
        void given_clientesVariasEdades_when_crearVarios_then_viabilidadCalculadaCorrectamente() {
            // Given & When & Then - Cliente 17 años (no viable)
            ClienteCreateDTO cliente17 = new ClienteCreateDTO(
                    "17000000", "Cliente", "Menor", LocalDate.of(2008, 7, 19),
                    "Ciudad", "menor@email.com", "3001111111", Ocupacion.EMPLEADO
            );
            ResponseEntity<String> response17 = restTemplate.exchange(
                    baseUrl, HttpMethod.POST, new HttpEntity<>(cliente17, headers), String.class
            );
            assertThat(response17.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response17.getBody()).contains("Es viable: No");
            
            // Cliente 18 años (viable)
            ClienteCreateDTO cliente18 = new ClienteCreateDTO(
                    "18000000", "Cliente", "JustoViable", LocalDate.of(2007, 7, 19),
                    "Ciudad", "viable18@email.com", "3002222222", Ocupacion.EMPLEADO
            );
            ResponseEntity<String> response18 = restTemplate.exchange(
                    baseUrl, HttpMethod.POST, new HttpEntity<>(cliente18, headers), String.class
            );
            assertThat(response18.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response18.getBody()).contains("Es viable: Sí");
            
            // Cliente 65 años (viable)
            ClienteCreateDTO cliente65 = new ClienteCreateDTO(
                    "65000000", "Cliente", "LimiteViable", LocalDate.of(1960, 7, 19),
                    "Ciudad", "viable65@email.com", "3003333333", Ocupacion.EMPLEADO
            );
            ResponseEntity<String> response65 = restTemplate.exchange(
                    baseUrl, HttpMethod.POST, new HttpEntity<>(cliente65, headers), String.class
            );
            assertThat(response65.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response65.getBody()).contains("Es viable: Sí");
            
            // Cliente 66 años (no viable)
            ClienteCreateDTO cliente66 = new ClienteCreateDTO(
                    "66000000", "Cliente", "Mayor", LocalDate.of(1959, 7, 19),
                    "Ciudad", "mayor@email.com", "3004444444", Ocupacion.PENSIONADO
            );
            ResponseEntity<String> response66 = restTemplate.exchange(
                    baseUrl, HttpMethod.POST, new HttpEntity<>(cliente66, headers), String.class
            );
            assertThat(response66.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response66.getBody()).contains("Es viable: No");

            // Verificar en BD que se guardaron correctamente las viabilidades
            clienteJPA.findById("17000000").ifPresentOrElse(
                cliente -> assertThat(cliente.getEsViable()).isFalse(),
                () -> fail("Cliente 17000000 debería existir en la base de datos")
            );
            clienteJPA.findById("18000000").ifPresentOrElse(
                cliente -> assertThat(cliente.getEsViable()).isTrue(),
                () -> fail("Cliente 18000000 debería existir en la base de datos")
            );
            clienteJPA.findById("65000000").ifPresentOrElse(
                cliente -> assertThat(cliente.getEsViable()).isTrue(),
                () -> fail("Cliente 65000000 debería existir en la base de datos")
            );
            clienteJPA.findById("66000000").ifPresentOrElse(
                cliente -> assertThat(cliente.getEsViable()).isFalse(),
                () -> fail("Cliente 66000000 debería existir en la base de datos")
            );
        }
    }
}

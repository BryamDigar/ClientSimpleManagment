package com.rti.prueba.controller.exception;

/**
 * Excepción lanzada cuando ya existe un cliente con el mismo número de documento o correo electrónico
 */
public class ClienteAlreadyExistsException extends RuntimeException {
    
    public ClienteAlreadyExistsException(String numeroDocumento) {
        super("Ya existe un cliente con el número de documento '" + numeroDocumento + "'");
    }
    
    public ClienteAlreadyExistsException(String field, String value) {
        super("Ya existe un cliente con " + field + " '" + value + "'");
    }
    
    public ClienteAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}

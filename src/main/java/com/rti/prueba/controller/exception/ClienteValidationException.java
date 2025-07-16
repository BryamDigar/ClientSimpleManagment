package com.rti.prueba.controller.exception;

/**
 * Excepción lanzada cuando los datos del cliente no son válidos
 */
public class ClienteValidationException extends RuntimeException {
    
    public ClienteValidationException(String message) {
        super(message);
    }

}

package com.rti.prueba.exception;

/**
 * Excepción lanzada cuando ocurre un error interno del servidor
 */
public class ClienteServiceException extends RuntimeException {

    public ClienteServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

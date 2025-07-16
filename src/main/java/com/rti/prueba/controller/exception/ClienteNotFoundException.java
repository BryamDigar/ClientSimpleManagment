package com.rti.prueba.controller.exception;

/**
 * Excepción lanzada cuando un cliente no es encontrado en la base de datos
 */
public class ClienteNotFoundException extends RuntimeException {

    public ClienteNotFoundException(String numeroDocumento) {
        super("Cliente con número de documento '" + numeroDocumento + "' no encontrado");
    }

}

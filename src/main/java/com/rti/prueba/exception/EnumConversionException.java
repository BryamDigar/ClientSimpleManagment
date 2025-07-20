package com.rti.prueba.exception;

/**
 * Excepción lanzada cuando ocurre un error en la conversión de enums
 */
public class EnumConversionException extends RuntimeException {

    public EnumConversionException(String enumType, String value) {
        super("Valor no válido para " + enumType + ": '" + value + "'");
    }
}

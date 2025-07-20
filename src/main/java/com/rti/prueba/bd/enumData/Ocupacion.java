package com.rti.prueba.bd.enumData;

import com.fasterxml.jackson.annotation.JsonValue;
import com.rti.prueba.controller.exception.EnumConversionException;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Ocupacion {
    EMPLEADO ("Empleado"),
    INDEPENDIENTE ("Independiente"),
    PENSIONADO ("Pensionado");

    @JsonValue
    private final String descripcion;

        public static Ocupacion fromDescripcion(String valor) {
        if (valor == null) return null;

        for (Ocupacion o : values()) {
            if (o.descripcion.equalsIgnoreCase(valor)) {
                return o;
            }
        }
        throw new EnumConversionException("Ocupacion", valor);
    }
}

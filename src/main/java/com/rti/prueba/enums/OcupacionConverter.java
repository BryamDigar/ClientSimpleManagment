package com.rti.prueba.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OcupacionConverter implements AttributeConverter<Ocupacion, String> {

    @Override
    public String convertToDatabaseColumn(Ocupacion ocupacion) {
        return ocupacion != null ? ocupacion.getDescripcion() : null;
    }

    @Override
    public Ocupacion convertToEntityAttribute(String descripcion) {
        if (descripcion == null) return null;
        return Ocupacion.fromDescripcion(descripcion);
    }
}
